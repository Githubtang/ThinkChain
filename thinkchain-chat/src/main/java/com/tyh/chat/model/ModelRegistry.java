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
 * 加载配置文件中已启用的模型，并按“逻辑模型名”建立内存索引。
 *
 * <p>例如请求传 qwen-plus，注册表会找到它对应的 provider、真实 modelName、能力和密钥。
 * 这样控制器不需要接收密钥，也不需要知道模型厂商的具体配置。</p>
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
        // @PostConstruct 会在 Spring 创建完本对象后自动执行一次，把 YAML 配置转换成运行时对象。
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
        // 对话主流程使用内部条目，因为厂商适配器真正发请求时需要 API Key。
        ModelEntry entry = registry.get(name);
        if (entry == null) {
            throw new IllegalArgumentException("Model not found or disabled: " + name);
        }
        return entry;
    }

    public List<ModelEntry> listModels() {
        return new ArrayList<>(registry.values());
    }

    public List<ModelSummary> listPublicModels() {
        // HTTP 模型列表只能使用公开摘要，主动丢弃 API Key 和 Base URL。
        return registry.values().stream()
                .map(ModelSummary::from)
                .sorted(java.util.Comparator.comparing(ModelSummary::name))
                .toList();
    }
}
