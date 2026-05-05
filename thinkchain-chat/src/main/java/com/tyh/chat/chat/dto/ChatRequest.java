package com.tyh.chat.chat.dto;

import java.util.List;

/**
 * 统一的聊天请求 DTO，与具体模型厂商实现解耦。
 *
 * @Author: GithubTang
 * @Description: 聊天请求
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class ChatRequest {

    /** 会话 ID，留空时由服务端创建新会话。 */
    private String conversationId;

    /** 当前用户 ID，未接入登录时允许为空。 */
    private String userId;

    /** 模型逻辑名称，对应 application-ai.yml 中的配置项。 */
    private String model;

    /** 本次会话或请求使用的系统提示词。 */
    private String systemPrompt;

    /** 对话消息列表。 */
    private List<Message> messages;

    /** 模型调用参数。 */
    private ModelCallOptions options;

    /** 是否启用流式响应。 */
    private Boolean stream;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public ModelCallOptions getOptions() {
        return options;
    }

    public void setOptions(ModelCallOptions options) {
        this.options = options;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }
}
