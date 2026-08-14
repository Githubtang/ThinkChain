package com.tyh.chat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 把 application-ai.yml 中 {@code langchain4j.multi-model.*} 配置自动绑定为 Java 对象。
 *
 * <p>Spring Boot 启动时负责完成 YAML 到对象的转换，业务代码不需要自己读取或解析配置文件。</p>
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
        /** 前端和接口传入的逻辑名称，例如 qwen-plus。 */
        private String name;
        /** 厂商标识，用于选择 VendorChatAdapter，例如 dashscope。 */
        private String provider;
        /** 调用厂商接口的密钥，只供服务端内部使用。 */
        private String apiKey;
        /** 厂商 API 基础地址；某些 SDK 可能不使用该字段。 */
        private String baseUrl;
        /** 厂商真正识别的模型名称。 */
        private String modelName;
        /** false 时不会加入 ModelRegistry，也不能被接口调用。 */
        private boolean enabled;
        /** 模型能力标签，例如 chat、text、image、document。 */
        private List<String> capabilities;
    }
}
