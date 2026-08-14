package com.tyh.chat.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyh.chat.properties.MultiModelProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ModelRegistryTest {

    @Test
    void publicModelListDoesNotExposeCredentialsOrBaseUrl() throws Exception {
        MultiModelProperties.ModelDefinition definition = new MultiModelProperties.ModelDefinition();
        definition.setName("qwen-plus");
        definition.setProvider("dashscope");
        definition.setApiKey("top-secret-key");
        definition.setBaseUrl("https://private.example.test");
        definition.setModelName("qwen-plus");
        definition.setEnabled(true);
        definition.setCapabilities(List.of("text", "chat"));

        MultiModelProperties properties = new MultiModelProperties();
        properties.setModels(List.of(definition));
        ModelRegistry registry = new ModelRegistry(properties);
        registry.init();

        String json = new ObjectMapper().writeValueAsString(registry.listPublicModels());

        assertThat(json).contains("qwen-plus", "dashscope", "text", "chat");
        assertThat(json).doesNotContain("top-secret-key", "private.example.test", "apiKey", "baseUrl");
    }

    @Test
    void modelEntryApiKeyIsIgnoredByJsonAsDefenseInDepth() throws Exception {
        ModelEntry entry = new ModelEntry(
                "model", "provider", "top-secret-key", "https://example.test", "upstream", Set.of("text"));

        String json = new ObjectMapper().writeValueAsString(entry);

        assertThat(json).doesNotContain("top-secret-key", "apiKey");
    }
}
