package com.tyh.chat.registry;

import com.tyh.chat.properties.MultiModelProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: GithubTang
 * @Description: 模型注册模块（启动时加载 enabled 模型，过滤 capabilities 中空项）
 * @Date: 2026/3/29 2:05
 * @Version: 1.0
 */
@Component
public class ModelRegistry {

    private final MultiModelProperties properties;

    private final Map<String, ModelEntry> registry = new ConcurrentHashMap<>();

    public ModelRegistry(MultiModelProperties properties) {
        this.properties = properties;
    }

    /** 启动时加载 {@code langchain4j.multi-model.models} 中已启用模型并写入内存注册表。 */
    @PostConstruct
    public void init() {
        if (properties.getModels() == null || properties.getModels().isEmpty()) {
            throw new IllegalStateException("No models configured in langchain4j.multi-model.models");
        }

        for (MultiModelProperties.ModelDefinition def : properties.getModels()) {
            if (def.isEnabled()) {
                Set<String> caps = new HashSet<>();
                if (def.getCapabilities() != null) {
                    for (String c : def.getCapabilities()) {
                        if (c != null && !c.isBlank()) {
                            caps.add(c.trim());
                        }
                    }
                }
                ModelEntry entry = new ModelEntry(def.getName(), def.getProvider(), def.getApiKey(),
                        def.getBaseUrl(), def.getModelName(), caps);
                registry.put(def.getName(), entry);
            }
        }
    }

    /**
     * 按逻辑模型名解析条目（未配置或 disabled 则抛异常）。
     *
     * @param name 配置中的 name
     */
    public ModelEntry getModel(String name) {
        ModelEntry entry = registry.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Model not found or disabled: " + name);
        }
        return entry;
    }

    /** @return 当前已注册的全部模型条目副本 */
    public List<ModelEntry> listModels() {
        return new ArrayList<>(registry.values());
    }

    /**
     * 单个已启用模型的运行时视图（供 {@link com.tyh.chat.vendor.VendorChatAdapter} 使用）。
     */
    public static class ModelEntry {
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
}
