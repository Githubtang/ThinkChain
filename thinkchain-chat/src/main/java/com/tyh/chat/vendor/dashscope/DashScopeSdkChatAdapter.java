package com.tyh.chat.vendor.dashscope;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionResult;
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionTaskResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.TaskStatus;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.Constants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.model.ModelEntry;
import com.tyh.chat.vendor.VendorChatAdapter;
import com.tyh.chat.vendor.VendorChatResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private static final String TRANSCRIPTION_MODEL = "paraformer-v2";
    private static final String DEFAULT_NATIVE_BASE_URL = "https://dashscope.aliyuncs.com/api/v1";
    private static final String COMPATIBLE_SUFFIX = "/compatible-mode/v1";
    private static final Object SDK_BASE_URL_LOCK = new Object();

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    public DashScopeSdkChatAdapter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        MultiModalMessage userMessage = buildUserMultiModalMessage(model, messages);
        MultiModalConversationParam param = MultiModalConversationParam.builder()
                .apiKey(model.getApiKey())
                .model(model.getModelName())
                .message(userMessage)
                .build();

        // 真正的外部网络调用发生在 conv.call；网络、鉴权或模型错误会向上抛给 AiChatService 统一处理。
        // 多模态 SDK 使用原生 /api/v1 地址，不能直接使用配置中的 /compatible-mode/v1 地址。
        MultiModalConversation conv = new MultiModalConversation(
                Protocol.HTTP.getValue(), nativeApiBaseUrl(model.getBaseUrl()));
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
    private MultiModalMessage buildUserMultiModalMessage(ModelEntry model, List<Message> messages) throws Exception {
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
                parts.addAll(toDashScopeContentParts(model, c));
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

    /**
     * 将一段用户内容转换成 DashScope 多模态消息片段。
     *
     * <p>这里保留直观的 switch case：视频和音频必须走不同分支。</p>
     * <ul>
     *     <li>{@code video}：使用 DashScope 原生 video 字段，让多模态模型读取视频画面；</li>
     *     <li>{@code audio}：先调用 Paraformer 转写，再把识别出的文字交给聊天模型。</li>
     * </ul>
     *
     * @param model   本次请求选择的 DashScope 模型配置
     * @param content 用户传入的一段文本或媒体内容
     * @return DashScope SDK 可以识别的内容片段
     */
    List<Map<String, Object>> toDashScopeContentParts(ModelEntry model, Content content) throws Exception {
        if (content == null) {
            throw new IllegalArgumentException("content 不能为空");
        }
        String type = content.getType() == null
                ? ""
                : content.getType().trim().toLowerCase(Locale.ROOT);

        return switch (type) {
            case "text" -> List.of(part("text", content.getText() != null ? content.getText() : ""));
            case "image" -> List.of(part("image", requiredReference(content, "image")));
            case "file", "document" -> {
                // 现有文档流程使用可访问 URL；以后若切换为 file-id，只需要修改此分支。
                yield List.of(part("image", requiredReference(content, type)));
            }
            case "video" -> {
                // 视频直接交给视觉模型读取，不进行音频转写。
                yield List.of(part("video", requiredReference(content, "video")));
            }
            case "audio" -> {
                // 音频先转写为文字，再作为文本加入本次对话。
                String transcript = transcribeAudio(model, requiredReference(content, "audio"));
                if (transcript == null || transcript.isBlank()) {
                    throw new IllegalStateException("DashScope 音频转写结果为空");
                }
                yield List.of(part("text", "[音频转写]\n" + transcript.trim()));
            }
            default -> throw new IllegalArgumentException("不支持的 content.type: " + content.getType());
        };
    }

    /**
     * 使用 DashScope Paraformer 把音频文件转写成文字。
     *
     * <p>处理过程是：验证音频 URL、提交异步转写任务、等待任务完成、下载结果 JSON、提取文字。
     * 当前聊天接口会同步等待转写完成，因此音频较长时请求耗时也会增加。</p>
     *
     * @param model    当前 DashScope 模型配置，复用其中的 API Key 和服务地址
     * @param audioUrl DashScope 能够访问的 HTTP/HTTPS 音频地址
     * @return 音频转写文字
     */
    String transcribeAudio(ModelEntry model, String audioUrl) throws Exception {
        validateAudioUrl(audioUrl);
        TranscriptionResult completedResult = executeTranscription(model, audioUrl.trim());
        if (completedResult == null || completedResult.getTaskStatus() != TaskStatus.SUCCEEDED) {
            throw new IllegalStateException("DashScope 音频转写任务未成功完成");
        }

        TranscriptionTaskResult taskResult = firstSuccessfulTask(completedResult.getResults());
        String resultJson = restTemplate.getForObject(taskResult.getTranscriptionUrl(), String.class);
        if (resultJson == null || resultJson.isBlank()) {
            throw new IllegalStateException("DashScope 音频转写结果文件为空");
        }

        String transcript = extractTranscript(objectMapper.readTree(resultJson));
        if (transcript.isBlank()) {
            throw new IllegalStateException("DashScope 音频转写结果中没有可用文字");
        }
        return transcript;
    }

    /** 提交 Paraformer 异步任务，并阻塞等待 DashScope 返回最终结果。 */
    private static TranscriptionResult executeTranscription(ModelEntry model, String audioUrl) {
        TranscriptionParam param = TranscriptionParam.builder()
                .apiKey(model.getApiKey())
                .model(TRANSCRIPTION_MODEL)
                .parameter("language_hints", new String[]{"zh", "en"})
                .fileUrls(List.of(audioUrl))
                .build();

        // 录音转写 SDK 使用全局服务地址，因此必须加锁并在调用结束后恢复原值。
        synchronized (SDK_BASE_URL_LOCK) {
            String previousBaseUrl = Constants.baseHttpApiUrl;
            try {
                Constants.baseHttpApiUrl = nativeApiBaseUrl(model.getBaseUrl());
                Transcription transcription = new Transcription();
                TranscriptionResult submitted = transcription.asyncCall(param);
                if (submitted == null || submitted.getTaskId() == null || submitted.getTaskId().isBlank()) {
                    throw new IllegalStateException("DashScope 未返回音频转写任务编号");
                }
                TranscriptionQueryParam query = TranscriptionQueryParam.FromTranscriptionParam(
                        param, submitted.getTaskId());
                return transcription.wait(query);
            } finally {
                Constants.baseHttpApiUrl = previousBaseUrl;
            }
        }
    }

    /** 找到成功且包含结果下载地址的音频子任务。 */
    private static TranscriptionTaskResult firstSuccessfulTask(List<TranscriptionTaskResult> results) {
        if (results != null) {
            for (TranscriptionTaskResult result : results) {
                if (result != null
                        && result.getSubTaskStatus() == TaskStatus.SUCCEEDED
                        && result.getTranscriptionUrl() != null
                        && !result.getTranscriptionUrl().isBlank()) {
                    return result;
                }
            }
        }
        throw new IllegalStateException("DashScope 音频转写任务没有可下载的成功结果");
    }

    /** 从 Paraformer 返回的 JSON 中按顺序提取 transcripts[].text。 */
    static String extractTranscript(JsonNode root) {
        if (root == null || root.isNull()) {
            return "";
        }

        Set<String> texts = new LinkedHashSet<>();
        JsonNode transcripts = root.path("transcripts");
        if (transcripts.isArray()) {
            for (JsonNode transcript : transcripts) {
                addText(texts, transcript.path("text"));
            }
        }
        // 兼容不同版本的返回结构：标准位置没有文字时，再递归寻找 text 字段。
        if (texts.isEmpty()) {
            for (JsonNode textNode : root.findValues("text")) {
                addText(texts, textNode);
            }
        }
        return String.join("\n", texts);
    }

    /** 只收集真正有内容的文本节点，并保持原有顺序。 */
    private static void addText(Set<String> texts, JsonNode textNode) {
        if (textNode != null && textNode.isTextual() && !textNode.asText().isBlank()) {
            texts.add(textNode.asText().trim());
        }
    }

    /** Paraformer 录音文件转写只接收可公开访问的 HTTP/HTTPS 地址。 */
    private static void validateAudioUrl(String audioUrl) {
        if (audioUrl == null || audioUrl.isBlank()) {
            throw new IllegalArgumentException("audio 类型需要提供音频 url");
        }
        URI uri;
        try {
            uri = URI.create(audioUrl.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("音频 url 格式不正确", ex);
        }
        String scheme = uri.getScheme();
        if (uri.getHost() == null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("音频 url 必须是 DashScope 可访问的 HTTP/HTTPS 地址");
        }
    }

    /** 把配置中的兼容接口地址转换成 DashScope SDK 使用的原生 /api/v1 地址。 */
    static String nativeApiBaseUrl(String configuredBaseUrl) {
        if (configuredBaseUrl == null || configuredBaseUrl.isBlank()) {
            return DEFAULT_NATIVE_BASE_URL;
        }
        String baseUrl = configuredBaseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (baseUrl.endsWith(COMPATIBLE_SUFFIX)) {
            return baseUrl.substring(0, baseUrl.length() - COMPATIBLE_SUFFIX.length()) + "/api/v1";
        }
        if (baseUrl.endsWith("/api/v1")) {
            return baseUrl;
        }
        return baseUrl + "/api/v1";
    }

    /** 从 url、text 中依次选择一个可用的媒体地址。 */
    private static String requiredReference(Content content, String type) {
        String reference = firstNonBlank(content.getUrl(), content.getText());
        if (reference == null) {
            throw new IllegalArgumentException(type + " 类型需要提供 url 或 text");
        }
        return reference.trim();
    }

    /** 构造单键值对 content 片段（满足 SDK 对 {@code Map<String, Object>} 的要求）。 */
    private static Map<String, Object> part(String key, Object value) {
        Map<String, Object> m = new HashMap<>(2);
        m.put(key, value);
        return m;
    }

    /** 返回第一个非空且非空白的字符串。 */
    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
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
