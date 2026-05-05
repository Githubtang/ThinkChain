package com.tyh.chat.chat.dto;

/**
 * 单次模型调用的可选参数。
 *
 * @Author: GithubTang
 * @Description: 模型调用参数
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class ModelCallOptions {

    /** 采样温度，值越大随机性越高。 */
    private Double temperature;

    /** Top-p 采样阈值。 */
    private Double topP;

    /** 最大输出 Token 数。 */
    private Integer maxTokens;

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}
