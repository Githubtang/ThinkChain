package com.tyh.chat.rag.log.domain;

import com.tyh.common.core.domain.BaseEntity;

import java.math.BigDecimal;

public class RagQueryLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String id;
    private String conversationId;
    private String knowledgeBaseId;
    private String knowledgeBaseIds;
    private String sessionDocumentIds;
    private String ragMode;
    private String userId;
    private String model;
    private String embeddingModel;
    private String question;
    private String rewrittenQuery;
    private String answer;
    private Integer topK;
    private BigDecimal minScore;
    private Integer hitCount;
    private String hitChunks;
    private Long elapsedMs;
    private String status;
    private String errorMessage;

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

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(String knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getKnowledgeBaseIds() {
        return knowledgeBaseIds;
    }

    public void setKnowledgeBaseIds(String knowledgeBaseIds) {
        this.knowledgeBaseIds = knowledgeBaseIds;
    }

    public String getSessionDocumentIds() {
        return sessionDocumentIds;
    }

    public void setSessionDocumentIds(String sessionDocumentIds) {
        this.sessionDocumentIds = sessionDocumentIds;
    }

    public String getRagMode() {
        return ragMode;
    }

    public void setRagMode(String ragMode) {
        this.ragMode = ragMode;
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

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getRewrittenQuery() {
        return rewrittenQuery;
    }

    public void setRewrittenQuery(String rewrittenQuery) {
        this.rewrittenQuery = rewrittenQuery;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public void setMinScore(BigDecimal minScore) {
        this.minScore = minScore;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

    public String getHitChunks() {
        return hitChunks;
    }

    public void setHitChunks(String hitChunks) {
        this.hitChunks = hitChunks;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
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
}
