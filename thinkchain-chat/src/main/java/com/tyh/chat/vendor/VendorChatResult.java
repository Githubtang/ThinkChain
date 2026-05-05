package com.tyh.chat.vendor;

/**
 * 统一的厂商对话结果，包含内容、Token 用量和原始响应。
 *
 * @Author: GithubTang
 * @Description: 厂商对话结果
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class VendorChatResult {

    private String content;

    private Integer promptTokens;

    private Integer completionTokens;

    private String rawResponse;

    public static VendorChatResult of(String content) {
        VendorChatResult result = new VendorChatResult();
        result.setContent(content);
        return result;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }
}
