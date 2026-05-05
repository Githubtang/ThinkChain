package com.tyh.chat.model;

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
 * 加载已启用模型，并按逻辑模型名建立索引。
 *
 * @Author: GithubTang
 * @Description: 模型注册表
 * @Date: 2026/4/29
 * @Version: 1.0
 */
@Component
public class ModelRegistry {

    private final MultiModelProperties properties;

    private final Map<String, ModelEntry> registry = new ConcurrentHashMap<>();

    public ModelRegistry(MultiModelProperties properties) {
        this.properties = properties;
    }

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

    public ModelEntry getModel(String name) {
        ModelEntry entry = registry.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Model not found or disabled: " + name);
        }
        return entry;
    }

    public List<ModelEntry> listModels() {
        return new ArrayList<>(registry.values());
    }
}
