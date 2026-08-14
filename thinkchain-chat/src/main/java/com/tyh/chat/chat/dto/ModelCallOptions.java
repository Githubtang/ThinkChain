package com.tyh.chat.chat.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
    @DecimalMin(value = "0.0", message = "temperature不能小于0")
    @DecimalMax(value = "2.0", message = "temperature不能大于2")
    private Double temperature;

    /** Top-p 采样阈值。 */
    @DecimalMin(value = "0.0", inclusive = false, message = "topP必须大于0")
    @DecimalMax(value = "1.0", message = "topP不能大于1")
    private Double topP;

    /** 最大输出 Token 数。 */
    @Min(value = 1, message = "maxTokens不能小于1")
    @Max(value = 32768, message = "maxTokens不能大于32768")
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
