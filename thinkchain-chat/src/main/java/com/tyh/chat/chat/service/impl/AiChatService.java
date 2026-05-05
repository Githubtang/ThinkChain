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
import com.tyh.chat.log.service.ModelCallLogService;
import com.tyh.chat.model.ModelEntry;
import com.tyh.chat.model.ModelRegistry;
import com.tyh.chat.vendor.VendorChatAdapter;
import com.tyh.chat.vendor.VendorChatAdapterRegistry;
import com.tyh.chat.vendor.VendorChatResult;
import com.tyh.common.core.domain.AjaxResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 编排模型注册、能力校验、厂商适配调用、会话持久化与调用日志记录。
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

    public AiChatService(ModelRegistry modelRegistry,
                         CapabilityValidator capabilityValidator,
                         VendorChatAdapterRegistry vendorChatAdapterRegistry,
                         ConversationService conversationService,
                         ModelCallLogService modelCallLogService) {
        this.modelRegistry = modelRegistry;
        this.capabilityValidator = capabilityValidator;
        this.vendorChatAdapterRegistry = vendorChatAdapterRegistry;
        this.conversationService = conversationService;
        this.modelCallLogService = modelCallLogService;
    }

    @Override
    public AjaxResult chat(ChatRequest request, Set<String> requiredCapabilities) {
        long start = System.currentTimeMillis();
        ModelEntry model = null;
        ChatMessage assistantMessage = null;
        try {
            if (request == null) {
                return AjaxResult.error("Request must not be null");
            }
            if (request.getModel() == null || request.getModel().isBlank()) {
                return AjaxResult.error("Model name must not be blank");
            }
            model = modelRegistry.getModel(request.getModel().trim());

            Set<String> required = new LinkedHashSet<>();
            if (requiredCapabilities != null) {
                required.addAll(requiredCapabilities);
            }
            required.addAll(ChatCapabilityDeriver.derive(request));
            capabilityValidator.validate(model, required);

            ChatConversation conversation = persistConversationQuietly(request);
            persistUserMessagesQuietly(request, conversation);

            VendorChatAdapter adapter = vendorChatAdapterRegistry.getRequired(model.getProvider());
            VendorChatResult result = adapter.invoke(model, request);
            long elapsedMs = System.currentTimeMillis() - start;
            assistantMessage = persistAssistantMessageQuietly(request, model, result);

            ChatResponse response = new ChatResponse();
            response.setConversationId(request.getConversationId());
            response.setMessageId(assistantMessage != null ? assistantMessage.getId() : null);
            response.setModel(model.getName());
            response.setProvider(model.getProvider());
            response.setContent(result.getContent());
            response.setElapsedMs(elapsedMs);
            response.setPromptTokens(result.getPromptTokens());
            response.setCompletionTokens(result.getCompletionTokens());
            recordCallLogQuietly(request, model, assistantMessage, result, "SUCCESS", null, elapsedMs);
            return AjaxResult.success(response);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            recordCallLogQuietly(request, model, assistantMessage, null, "FAILED", e.getMessage(), System.currentTimeMillis() - start);
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("Model invocation failed", e);
            recordCallLogQuietly(request, model, assistantMessage, null, "FAILED", e.getMessage(), System.currentTimeMillis() - start);
            return AjaxResult.error("Model invocation failed: " + e.getMessage());
        }
    }

    @Override
    public AjaxResult chat(String modelName, String userInput, Set<String> requiredCapabilities) {
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
        try {
            return conversationService.ensureConversation(request);
        } catch (Exception e) {
            log.warn("Failed to persist conversation; continuing chat call: {}", e.getMessage());
            return null;
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
            logRecord.setRequestBody(request != null ? request.toString() : null);
            logRecord.setResponseBody(result != null ? result.getRawResponse() : null);
            logRecord.setStatus(status);
            logRecord.setErrorMessage(errorMessage);
            logRecord.setElapsedMs(elapsedMs);
            modelCallLogService.record(logRecord);
        } catch (Exception e) {
            log.warn("Failed to record model call log: {}", e.getMessage());
        }
    }
}
