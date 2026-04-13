package com.tyh.chat.capability;

import com.tyh.chat.registry.ModelRegistry;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @Author: GithubTang
 * @Description: 校验模型是否具备请求所需的 capability（与 application-ai.yml 中 capabilities 对齐；空集合跳过）
 * @Date: 2026/3/29 2:07
 * @Version: 1.0
 */
@Component
public class CapabilityValidator {

    /**
     * 校验模型条目是否支持全部所需能力。
     *
     * @param model                  已注册的模型
     * @param requiredCapabilities   所需能力标签，null 或 empty 时不校验
     */
    public void validate(ModelRegistry.ModelEntry model, Set<String> requiredCapabilities) {
        if (requiredCapabilities == null || requiredCapabilities.isEmpty()) {
            return;
        }
        for (String cap : requiredCapabilities) {
            if (cap == null || cap.isBlank()) {
                continue;
            }
            String key = cap.trim();
            if (!model.supportsCapability(key)) {
                throw new IllegalArgumentException("Model " + model.getName() + " does not support required capability: " + key);
            }
        }
    }
}
