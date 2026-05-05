package com.tyh.chat.chat.dto;

/**
 * 统一的聊天响应，包含会话、消息、模型和调用统计信息。
 *
 * @Author: GithubTang
 * @Description: 聊天响应
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class ChatResponse {

    /** 会话 ID。 */
    private String conversationId;

    /** 当前回复消息 ID。 */
    private String messageId;

    /** 实际调用的模型名称。 */
    private String model;

    /** 模型提供方。 */
    private String provider;

    /** 模型返回的文本内容。 */
    private String content;

    /** 本次调用耗时（毫秒）。 */
    private Long elapsedMs;

    /** 输入 Token 数。 */
    private Integer promptTokens;

    /** 输出 Token 数。 */
    private Integer completionTokens;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }
}
