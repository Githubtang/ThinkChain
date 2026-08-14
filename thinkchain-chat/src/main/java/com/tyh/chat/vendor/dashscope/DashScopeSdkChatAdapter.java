package com.tyh.chat.vendor.dashscope;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.model.ModelEntry;
import com.tyh.chat.vendor.VendorChatAdapter;
import com.tyh.chat.vendor.VendorChatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 阿里云 DashScope 官方 Java SDK 适配器：通过 {@link MultiModalConversation} 调用多模态对话，
 * 将统一 {@link com.tyh.chat.chat.dto.ChatRequest} 转为 {@link MultiModalMessage}。
 *
 * <p>当前实现会把历史消息整理成文本前缀，把最后一条 user 消息保留为多模态片段。
 * ModelCallOptions 暂未映射到 SDK 参数，后续若需要 temperature、topP 等参数，应在构造 param 时补充。</p>
 *
 * @Author: GithubTang
 * @Description: DashScope 官方 SDK 多模态对话适配实现
 * @Date: 2026/4/11
 * @Version: 1.0
 */
@Component
public class DashScopeSdkChatAdapter implements VendorChatAdapter {

    private static final Logger log = LoggerFactory.getLogger(DashScopeSdkChatAdapter.class);

    @Override
    public String providerId() {
        return "dashscope";
    }

    /**
     * 调用 DashScope 多模态对话接口，将 {@link ChatRequest} 转为单次 {@link MultiModalMessage} 请求。
     */
    @Override
    public VendorChatResult invoke(ModelEntry model, ChatRequest request) throws Exception {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(request, "request");
        List<Message> messages = request.getMessages();
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }

        // 领域 DTO 不能直接传给厂商 SDK，需要先转换成 DashScope 的消息结构。
        MultiModalMessage userMessage = buildUserMultiModalMessage(messages);
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(model.getApiKey())
                .model(model.getModelName())
                .message(userMessage)
                .build();

        // 真正的外部网络调用发生在 conv.call；网络、鉴权或模型错误会向上抛给 AiChatService 统一处理。
        MultiModalConversation conv = new MultiModalConversation();
        MultiModalConversationResult result = conv.call(param);
        String text = extractAssistantText(result);
        if (text == null || text.isBlank()) {
            log.warn("DashScope 返回空文本, model={}", model.getName());
        }
        // 返回统一结果，避免上层代码依赖 DashScope 的 MultiModalConversationResult 类型。
        VendorChatResult chatResult = VendorChatResult.of(text != null ? text : "");
        chatResult.setRawResponse(result != null ? result.toString() : null);
        return chatResult;
    }

    /**
     * 将多条消息折叠为一条 user 多模态消息：此前的对话拼成前置文本，最后一条 user 的多模态片段接在后面。
     */
    private MultiModalMessage buildUserMultiModalMessage(List<Message> messages) {
        int lastUserIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            if (m != null && m.getRole() != null && "user".equalsIgnoreCase(m.getRole().trim())) {
                lastUserIndex = i;
                break;
            }
        }
        if (lastUserIndex < 0) {
            throw new IllegalArgumentException("至少需要一条 role=user 的消息");
        }

        StringBuilder prefix = new StringBuilder();
        for (int i = 0; i < lastUserIndex; i++) {
            Message m = messages.get(i);
            if (m == null || m.getRole() == null) {
                continue;
            }
            String line = flattenTextOnly(m);
            if (!line.isBlank()) {
                prefix.append(m.getRole().trim()).append(": ").append(line).append('\n');
            }
        }

        List<Map<String, Object>> parts = new ArrayList<>();
        if (!prefix.isEmpty()) {
            parts.add(part("text", prefix.toString()));
        }
        Message lastUser = messages.get(lastUserIndex);
        List<Content> contents = lastUser.getContents();
        if (contents == null || contents.isEmpty()) {
            parts.add(part("text", ""));
        } else {
            for (Content c : contents) {
                parts.addAll(toDashScopeContentParts(c));
            }
        }

        return MultiModalMessage.builder()
                .role("user")
                .content(parts)
                .build();
    }

    /** 仅从消息中提取 type=text 的片段拼接为纯文本（用于历史上下文前缀）。 */
    private static String flattenTextOnly(Message m) {
        if (m.getContents() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Content c : m.getContents()) {
            if (c == null || c.getType() == null) {
                continue;
            }
            if ("text".equalsIgnoreCase(c.getType().trim()) && c.getText() != null) {
                sb.append(c.getText());
            }
        }
        return sb.toString();
    }

    /** 将领域 {@link Content} 转为 DashScope 多模态 content 列表项。 */
    private List<Map<String, Object>> toDashScopeContentParts(Content c) {
        String type = c.getType() == null ? "" : c.getType().trim().toLowerCase(Locale.ROOT);
        return switch (type) {
            case "text" -> List.of(part("text", c.getText() != null ? c.getText() : ""));
            case "image" -> {
                String ref = firstNonBlank(c.getUrl(), c.getText());
                if (ref == null || ref.isBlank()) {
                    throw new IllegalArgumentException("image 类型需要提供 url 或 text(存放 base64/dataURL)");
                }
                yield List.of(part("image", ref.trim()));
            }
            case "file", "document" -> {
                String ref = firstNonBlank(c.getUrl(), c.getText());
                if (ref == null || ref.isBlank()) {
                    throw new IllegalArgumentException(type + " 类型需要提供 url 或 text(存放可访问地址)");
                }
                // 文档/文件按官方示例多使用可访问 URL；若后续需 file-id 可在此扩展
                yield List.of(part("image", ref.trim()));
            }
            case "video", "audio" -> {
                String ref = firstNonBlank(c.getUrl(), c.getText());
                if (ref == null || ref.isBlank()) {
                    throw new IllegalArgumentException(type + " 类型需要提供 url 或 text");
                }
                // 不同模型对音视频字段要求不同，先以文本引用方式传入，避免错误 content key 导致调用失败
                yield List.of(part("text", "[" + type + "] " + ref.trim()));
            }
            default -> throw new IllegalArgumentException("不支持的 content.type: " + c.getType());
        };
    }

    /** 构造单键值对 content 片段（满足 SDK 对 {@code Map<String, Object>} 的要求）。 */
    private static Map<String, Object> part(String key, Object value) {
        Map<String, Object> m = new HashMap<>(2);
        m.put(key, value);
        return m;
    }

    /** 返回第一个非空非空白字符串。 */
    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }

    /** 从 SDK 返回结果中解析助手文本（聚合 content 中 {@code text} 字段）。 */
    private static String extractAssistantText(MultiModalConversationResult result) {
        if (result == null || result.getOutput() == null) {
            return "";
        }
        var choices = result.getOutput().getChoices();
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        var message = choices.get(0).getMessage();
        if (message == null) {
            return "";
        }
        List<Map<String, Object>> content = message.getContent();
        if (content == null || content.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> part : content) {
            if (part == null) {
                continue;
            }
            Object t = part.get("text");
            if (t != null) {
                sb.append(t);
            }
        }
        return sb.toString();
    }
}
