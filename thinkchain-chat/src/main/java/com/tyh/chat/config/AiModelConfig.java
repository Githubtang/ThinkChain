package com.tyh.chat.config;

import com.tyh.chat.properties.MultiModelProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: GithubTang
 * @Description: 模型配置
 * @Date: 2026/3/18 17:52
 * @Version: 1.0
 */
@Configuration
@EnableConfigurationProperties(MultiModelProperties.class)
public class AiModelConfig {
//
//    private static final Logger log = LoggerFactory.getLogger(AiModelConfig.class);
//
//    @Bean
//    public ChatModel chatModel(MultiModelProperties properties) {
//        log.info("Attempting to create ChatModel bean.");
//
//        if (properties == null
//                || !properties.isEnabled()
//                || properties.getModels() == null
//                || properties.getModels().isEmpty()) {
//            log.warn("MultiModelProperties is not enabled or models are empty. Creating single model.");
//            return createSingleModel(properties == null ? null : properties.getActiveModel(), properties);
//        }
//
//        List<MultiModelProperties.ModelDefinition> defs = properties.getModels();
//
//        // Lower number => higher priority
//        defs.sort(Comparator.comparingInt(MultiModelProperties.ModelDefinition::getPriority));
//
//        List<QuotaAwareChatModel.ModelEntry> entries = defs.stream()
//                .map(def -> {
//                    ChatModel delegate = createOpenAiCompatibleModel(def);
//                    return QuotaAwareChatModel.entry(
//                            def.getName(),
//                            def.getPriority(),
//                            def.getFreeLimit(),
//                            delegate,
//                            def.getModelName()
//                    );
//                })
//                .toList();
//
//        QuotaAwareChatModel quotaAwareChatModel = new QuotaAwareChatModel(entries, properties.getActiveModel());
//        log.info("Successfully created QuotaAwareChatModel bean with {} entries.", entries.size());
//        return quotaAwareChatModel;
//    }
//
//    private ChatModel createSingleModel(String activeModelName, MultiModelProperties properties) {
//        if (properties == null || properties.getModels() == null || properties.getModels().isEmpty()) {
//            throw new IllegalArgumentException("No model configured: langchain4j.multi-model.models is empty");
//        }
//        if (activeModelName == null || activeModelName.isBlank()) {
//            return createOpenAiCompatibleModel(properties.getModels().get(0));
//        }
//
//        return properties.getModels().stream()
//                .filter(def -> Objects.equals(def.getName(), activeModelName))
//                .findFirst()
//                .map(this::createOpenAiCompatibleModel)
//                .orElseGet(() -> createOpenAiCompatibleModel(properties.getModels().get(0)));
//    }
//
//    /**
//     * For OpenAI-compatible endpoints, we can reuse {@link OpenAiChatModel}.
//     * Your `application.yml` currently configures OpenAI/DashScope/ZhiPu using compatible-mode URLs.
//     */
//    private ChatModel createOpenAiCompatibleModel(MultiModelProperties.ModelDefinition def) {
//        if (def == null) {
//            throw new IllegalArgumentException("model definition is null");
//        }
//        if (def.getApiKey() == null || def.getApiKey().isBlank()) {
//            throw new IllegalArgumentException("model api-key is empty, name=" + def.getName());
//        }
//        if (def.getModelName() == null || def.getModelName().isBlank()) {
//            throw new IllegalArgumentException("model-name is empty, name=" + def.getName());
//        }
//
//        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
//                .apiKey(def.getApiKey())
//                .modelName(def.getModelName());
//
//        if (def.getBaseUrl() != null && !def.getBaseUrl().isBlank()) {
//            builder = builder.baseUrl(def.getBaseUrl());
//        }
//        return builder.build();
//    }
}