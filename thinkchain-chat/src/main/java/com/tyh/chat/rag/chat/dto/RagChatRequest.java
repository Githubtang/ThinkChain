package com.tyh.chat.rag.chat.dto;

import com.tyh.chat.chat.dto.ModelCallOptions;

import java.util.List;

public class RagChatRequest {

    private String conversationId;
    private String userId;
    private String model;
    private String question;
    private List<String> knowledgeBaseIds;
    private List<String> sessionDocumentIds;
    private String ragMode;
    private Integer topK;
    private ModelCallOptions options;

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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public List<String> getKnowledgeBaseIds() {
        return knowledgeBaseIds;
    }

    public void setKnowledgeBaseIds(List<String> knowledgeBaseIds) {
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    public List<String> getSessionDocumentIds() {
        return sessionDocumentIds;
    }

    public void setSessionDocumentIds(List<String> sessionDocumentIds) {
        this.sessionDocumentIds = sessionDocumentIds;
    }

    public String getRagMode() {
        return ragMode;
    }

    public void setRagMode(String ragMode) {
        this.ragMode = ragMode;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public ModelCallOptions getOptions() {
        return options;
    }

    public void setOptions(ModelCallOptions options) {
        this.options = options;
    }
}
