package com.tyh.chat.conversation.domain;

import com.tyh.common.core.domain.BaseEntity;

/**
 * AI 对话消息实体。
 *
 * @Author: GithubTang
 * @Description: 对话消息
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class ChatMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String id;

    private String conversationId;

    private String role;

    private String contentType;

    private String content;

    private String rawContent;

    private String model;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRawContent() {
        return rawContent;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
