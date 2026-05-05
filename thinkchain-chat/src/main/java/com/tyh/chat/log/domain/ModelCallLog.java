package com.tyh.chat.log.domain;

import com.tyh.common.core.domain.BaseEntity;

/**
 * 模型调用日志实体。
 *
 * @Author: GithubTang
 * @Description: 模型调用日志
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class ModelCallLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String id;

    private String conversationId;

    private String messageId;

    private String model;

    private String provider;

    private String requestBody;

    private String responseBody;

    private String status;

    private String errorMessage;

    private Long elapsedMs;

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

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }
}
