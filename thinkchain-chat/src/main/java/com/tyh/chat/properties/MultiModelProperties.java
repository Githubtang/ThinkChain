package com.tyh.chat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 绑定配置前缀 {@code langchain4j.multi-model.*}，读取多模型列表（name、provider、apiKey、capabilities 等）。
 *
 * @Author: GithubTang
 * @Description: 多模型 YAML 配置属性绑定
 * @Date: 2026/3/29
 * @Version: 1.0
 */
@ConfigurationProperties(prefix = "langchain4j.multi-model")
public class MultiModelProperties {

    private List<ModelDefinition> models;

    public List<ModelDefinition> getModels() {
        return models;
    }

    public void setModels(List<ModelDefinition> models) {
        this.models = models;
    }

    @Data
    public static class ModelDefinition {
        private String name;
        private String provider;
        private String apiKey;
        private String baseUrl;
        private String modelName;
        private boolean enabled;
        private List<String> capabilities; // 能力标签
    }
}
