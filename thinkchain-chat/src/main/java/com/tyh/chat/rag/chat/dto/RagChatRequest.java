package com.tyh.chat.rag.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tyh.chat.chat.dto.ModelCallOptions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RagChatRequest {

    @Size(max = 64, message = "会话ID长度不能超过64个字符")
    private String conversationId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String userId;
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String model;
    @NotBlank(message = "问题不能为空")
    @Size(max = 20000, message = "问题长度不能超过20000个字符")
    private String question;
    @Size(max = 20, message = "单次最多选择20个知识库")
    private List<String> knowledgeBaseIds;
    @Size(max = 50, message = "单次最多选择50个会话文档")
    private List<String> sessionDocumentIds;
    @Pattern(regexp = "(?i)AUTO|SESSION_ONLY|KB_ONLY|SESSION_AND_KB|NONE",
            message = "RAG模式不合法")
    private String ragMode;
    @Min(value = 1, message = "topK不能小于1")
    @Max(value = 50, message = "topK不能大于50")
    private Integer topK;
    @DecimalMin(value = "-1.0", message = "最低相似度不能小于-1")
    @DecimalMax(value = "1.0", message = "最低相似度不能大于1")
    private Double minScore;
    @Valid
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

    public Double getMinScore() {
        return minScore;
    }

    public void setMinScore(Double minScore) {
        this.minScore = minScore;
    }

    public ModelCallOptions getOptions() {
        return options;
    }

    public void setOptions(ModelCallOptions options) {
        this.options = options;
    }
}
