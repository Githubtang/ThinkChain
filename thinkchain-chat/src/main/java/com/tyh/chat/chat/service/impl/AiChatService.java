package com.tyh.chat.chat.service.impl;

import com.tyh.chat.capability.CapabilityValidator;
import com.tyh.chat.capability.ChatCapabilityDeriver;
import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.ChatResponse;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.chat.service.ChatService;
import com.tyh.chat.conversation.domain.ChatConversation;
import com.tyh.chat.conversation.domain.ChatMessage;
import com.tyh.chat.conversation.service.ConversationService;
import com.tyh.chat.log.domain.ModelCallLog;
import com.tyh.chat.log.ChatLogSanitizer;
import com.tyh.chat.log.service.ModelCallLogService;
import com.tyh.chat.model.ModelEntry;
import com.tyh.chat.model.ModelRegistry;
import com.tyh.chat.rag.retrieval.RagContextService;
import com.tyh.chat.vendor.VendorChatAdapter;
import com.tyh.chat.vendor.VendorChatAdapterRegistry;
import com.tyh.chat.vendor.VendorChatResult;
import com.tyh.common.core.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ChatService} 的主要实现，也是普通 AI 对话的业务编排中心。
 *
 * <p>这里的“编排”是把多个独立组件按顺序组合起来：模型注册表负责查找配置，
 * 能力校验器检查模型是否支持文本或图片，厂商适配器负责真正调用模型，
 * 会话服务保存消息，日志服务保存调用过程。</p>
 *
 * <p>本类不直接编写任何厂商 SDK 调用，因此增加新模型厂商时通常只需增加 VendorChatAdapter，
 * 不需要复制整套会话和日志逻辑。</p>
 *
 * @Author: GithubTang
 * @Description: AI 对话编排服务
 * @Date: 2026/4/29
 * @Version: 1.0
 */
@Service
public class AiChatService implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final ModelRegistry modelRegistry;
    private final CapabilityValidator capabilityValidator;
    private final VendorChatAdapterRegistry vendorChatAdapterRegistry;
    private final ConversationService conversationService;
    private final ModelCallLogService modelCallLogService;
    private final ObjectProvider<RagContextService> ragContextServiceProvider;
    private final ChatLogSanitizer logSanitizer;

    public AiChatService(ModelRegistry modelRegistry,
                         CapabilityValidator capabilityValidator,
                         VendorChatAdapterRegistry vendorChatAdapterRegistry,
                         ConversationService conversationService,
                         ModelCallLogService modelCallLogService,
                         ObjectProvider<RagContextService> ragContextServiceProvider,
                         ChatLogSanitizer logSanitizer) {
        this.modelRegistry = modelRegistry;
        this.capabilityValidator = capabilityValidator;
        this.vendorChatAdapterRegistry = vendorChatAdapterRegistry;
        this.conversationService = conversationService;
        this.modelCallLogService = modelCallLogService;
        this.ragContextServiceProvider = ragContextServiceProvider;
        this.logSanitizer = logSanitizer;
    }

    @Override
    public AjaxResult chat(ChatRequest request, Set<String> requiredCapabilities) {
        long start = System.currentTimeMillis();
        ModelEntry model = null;
        ChatMessage assistantMessage = null;
        try {
            // 1. HTTP 接口虽然已经校验参数，但服务也可能被其他 Java 代码直接调用，因此仍做防御性检查。
            if (request == null) {
                return AjaxResult.error("Request must not be null");
            }
            if (request.getModel() == null || request.getModel().isBlank()) {
                return AjaxResult.error("Model name must not be blank");
            }
            // 2. 逻辑模型名对应 application-ai.yml 中的一项配置；注册表不会把密钥返回给控制器。
            model = modelRegistry.getModel(request.getModel().trim());

            // 3. 合并接口要求和消息内容推导出的能力，防止使用纯文本模型处理图片等内容。
            Set<String> required = new LinkedHashSet<>();
            if (requiredCapabilities != null) {
                required.addAll(requiredCapabilities);
            }
            required.addAll(ChatCapabilityDeriver.derive(request));
            capabilityValidator.validate(model, required);

            // 4. 先尽力保存会话和用户消息。保存失败不会阻止模型回答。
            ChatConversation conversation = persistConversationQuietly(request);
            persistUserMessagesQuietly(request, conversation);

            // 5. 根据 provider 找厂商适配器；RAG 开启时先把检索资料加入模型请求。
            VendorChatAdapter adapter = vendorChatAdapterRegistry.getRequired(model.getProvider());
            ChatRequest modelRequest = augmentWithRagQuietly(request);
            VendorChatResult result = adapter.invoke(model, modelRequest);
            long elapsedMs = System.currentTimeMillis() - start;
            assistantMessage = persistAssistantMessageQuietly(request, model, result);

            // 6. 厂商结果转换成项目统一响应，前端不需要理解每个厂商不同的返回结构。
            ChatResponse response = new ChatResponse();
            response.setConversationId(request.getConversationId());
            response.setMessageId(assistantMessage != null ? assistantMessage.getId() : null);
            response.setModel(model.getName());
            response.setProvider(model.getProvider());
            response.setContent(result.getContent());
            response.setElapsedMs(elapsedMs);
            response.setPromptTokens(result.getPromptTokens());
            response.setCompletionTokens(result.getCompletionTokens());
            // 7. 日志记录采用“尽力而为”；日志失败不能覆盖已经成功得到的模型答案。
            recordCallLogQuietly(request, model, assistantMessage, result, "SUCCESS", null, elapsedMs);
            return AjaxResult.success(response);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            recordCallLogQuietly(request, model, assistantMessage, null, "FAILED", e.getMessage(), System.currentTimeMillis() - start);
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("Model invocation failed", e);
            recordCallLogQuietly(request, model, assistantMessage, null, "FAILED", e.getMessage(), System.currentTimeMillis() - start);
            return AjaxResult.error("模型调用失败，请稍后重试");
        }
    }

    @Override
    public AjaxResult chat(String modelName, String userInput, Set<String> requiredCapabilities) {
        // 便捷入口只负责把纯文本转换成统一 DTO，后续仍走上面的完整流程。
        ChatRequest request = new ChatRequest();
        request.setModel(modelName);
        Message message = new Message();
        message.setRole("user");
        Content content = new Content();
        content.setType("text");
        content.setText(userInput != null ? userInput : "");
        List<Message> messages = new ArrayList<>();
        message.setContents(List.of(content));
        messages.add(message);
        request.setMessages(messages);
        return chat(request, requiredCapabilities);
    }

    private ChatConversation persistConversationQuietly(ChatRequest request) {
        // Quietly 表示该辅助方法会记录警告，但不会把持久化异常继续抛给模型调用流程。
        try {
            return conversationService.ensureConversation(request);
        } catch (Exception e) {
            log.warn("Failed to persist conversation; continuing chat call: {}", e.getMessage());
            return null;
        }
    }

    private ChatRequest augmentWithRagQuietly(ChatRequest request) {
        try {
            RagContextService service = ragContextServiceProvider.getIfAvailable();
            // ObjectProvider 允许未配置 RAG 组件时继续使用普通聊天。
            return service != null ? service.augment(request) : request;
        } catch (Exception e) {
            log.warn("Failed to augment chat request with RAG context; continuing without RAG: {}", e.getMessage());
            return request;
        }
    }

    private void persistUserMessagesQuietly(ChatRequest request, ChatConversation conversation) {
        if (conversation == null || request.getMessages() == null) {
            return;
        }
        for (Message message : request.getMessages()) {
            if (message != null && "user".equalsIgnoreCase(message.getRole())) {
                try {
                    conversationService.saveUserMessage(conversation.getId(), request.getModel(), message);
                } catch (Exception e) {
                    log.warn("Failed to persist user message; continuing chat call: {}", e.getMessage());
                }
            }
        }
    }

    private ChatMessage persistAssistantMessageQuietly(ChatRequest request, ModelEntry model, VendorChatResult result) {
        if (request.getConversationId() == null || result == null) {
            return null;
        }
        try {
            return conversationService.saveAssistantMessage(request.getConversationId(), model.getName(), result.getContent(), result.getRawResponse());
        } catch (Exception e) {
            log.warn("Failed to persist assistant message; continuing chat call: {}", e.getMessage());
            return null;
        }
    }

    private void recordCallLogQuietly(ChatRequest request, ModelEntry model, ChatMessage message,
                                      VendorChatResult result, String status, String errorMessage, Long elapsedMs) {
        try {
            ModelCallLog logRecord = new ModelCallLog();
            logRecord.setConversationId(request != null ? request.getConversationId() : null);
            logRecord.setMessageId(message != null ? message.getId() : null);
            logRecord.setModel(model != null ? model.getName() : request != null ? request.getModel() : null);
            logRecord.setProvider(model != null ? model.getProvider() : null);
            // 写数据库前做 JSON 序列化、密钥脱敏和长度截断，避免日志泄密或超过字段长度。
            logRecord.setRequestBody(logSanitizer.serializeRequest(request));
            logRecord.setResponseBody(result != null ? logSanitizer.sanitizeResponse(result.getRawResponse()) : null);
            logRecord.setStatus(status);
            logRecord.setErrorMessage(logSanitizer.sanitizeError(errorMessage));
            logRecord.setElapsedMs(elapsedMs);
            modelCallLogService.record(logRecord);
        } catch (Exception e) {
            log.warn("Failed to record model call log: {}", e.getMessage());
        }
    }
}
