package com.tyh.chat.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
    @Size(max = 64, message = "会话ID长度不能超过64个字符")
    private String conversationId;

    /** 当前用户 ID，未接入登录时允许为空。 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String userId;

    /** 模型逻辑名称，对应 application-ai.yml 中的配置项。 */
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称长度不能超过100个字符")
    private String model;

    /** 本次会话或请求使用的系统提示词。 */
    @Size(max = 20000, message = "系统提示词长度不能超过20000个字符")
    private String systemPrompt;

    /** 对话消息列表。 */
    @NotEmpty(message = "消息列表不能为空")
    @Size(max = 100, message = "单次请求消息数量不能超过100条")
    @Valid
    private List<Message> messages;

    /** 模型调用参数。 */
    @Valid
    private ModelCallOptions options;

    /** 是否启用流式响应。 */
    private Boolean stream;

    /** 是否启用 RAG；为空时由 ragMode 和上下文选择自动判断。 */
    private Boolean ragEnabled;

    /** 本次对话显式选择的知识库 ID 列表。 */
    @Size(max = 20, message = "单次最多选择20个知识库")
    private List<String> knowledgeBaseIds;

    /** 当前会话上传并可参与检索的临时文档 ID 列表。 */
    @Size(max = 50, message = "单次最多选择50个会话文档")
    private List<String> sessionDocumentIds;

    /** RAG 召回数量。 */
    @Min(value = 1, message = "RAG topK不能小于1")
    @Max(value = 50, message = "RAG topK不能大于50")
    private Integer ragTopK;

    /** RAG 模式：AUTO / SESSION_ONLY / KB_ONLY / SESSION_AND_KB / NONE。 */
    @Pattern(regexp = "(?i)AUTO|SESSION_ONLY|KB_ONLY|SESSION_AND_KB|NONE",
            message = "RAG模式不合法")
    private String ragMode;

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

    public Boolean getRagEnabled() {
        return ragEnabled;
    }

    public void setRagEnabled(Boolean ragEnabled) {
        this.ragEnabled = ragEnabled;
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

    public Integer getRagTopK() {
        return ragTopK;
    }

    public void setRagTopK(Integer ragTopK) {
        this.ragTopK = ragTopK;
    }

    public String getRagMode() {
        return ragMode;
    }

    public void setRagMode(String ragMode) {
        this.ragMode = ragMode;
    }
}
