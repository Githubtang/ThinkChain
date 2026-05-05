package com.tyh.chat.conversation.domain;

import com.tyh.common.core.domain.BaseEntity;

/**
 * AI 对话会话实体。
 *
 * @Author: GithubTang
 * @Description: 对话会话
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class ChatConversation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String id;

    private String userId;

    private String title;

    private String model;

    private String systemPrompt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
}
