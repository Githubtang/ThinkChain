package com.tyh.chat.model;

import java.util.Set;

/**
 * 已启用模型的运行时视图。
 *
 * @Author: GithubTang
 * @Description: 模型注册表条目
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class ModelEntry {

    private final String name;
    private final String provider;
    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private final Set<String> capabilities;

    public ModelEntry(String name, String provider, String apiKey, String baseUrl, String modelName, Set<String> capabilities) {
        this.name = name;
        this.provider = provider;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.capabilities = capabilities;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModelName() {
        return modelName;
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }

    public boolean supportsCapability(String cap) {
        return capabilities.contains(cap);
    }
}
