package com.tyh.chat.rag.embedding.client;

import com.tyh.chat.rag.embedding.config.RagEmbeddingProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class DashScopeEmbeddingClient implements EmbeddingClient {

    private static final String EMBEDDING_PATH = "/api/v1/services/embeddings/text-embedding/text-embedding";

    private final RagEmbeddingProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public DashScopeEmbeddingClient(RagEmbeddingProperties properties) {
        this.properties = properties;
    }

    @Override
    public String modelName() {
        return properties.getModelName();
    }

    @Override
    public float[] embed(String text) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("DashScope embedding apiKey is blank");
        }
        String url = trimTrailingSlash(properties.getBaseUrl()) + EMBEDDING_PATH;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        Map<String, Object> body = Map.of(
                "model", properties.getModelName(),
                "input", Map.of("texts", List.of(text != null ? text : "")),
                "parameters", Map.of("dimension", properties.getDimensions())
        );
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
        return extractEmbedding(response);
    }

    @SuppressWarnings("unchecked")
    private static float[] extractEmbedding(Map<String, Object> response) {
        if (response == null) {
            throw new IllegalStateException("DashScope embedding response is empty");
        }
        Object outputObj = response.get("output");
        if (!(outputObj instanceof Map<?, ?> output)) {
            throw new IllegalStateException("DashScope embedding response missing output");
        }
        Object embeddingsObj = output.get("embeddings");
        if (!(embeddingsObj instanceof List<?> embeddings) || embeddings.isEmpty()) {
            throw new IllegalStateException("DashScope embedding response missing embeddings");
        }
        Object firstObj = embeddings.get(0);
        if (!(firstObj instanceof Map<?, ?> first)) {
            throw new IllegalStateException("DashScope embedding item is invalid");
        }
        Object vectorObj = first.get("embedding");
        if (!(vectorObj instanceof List<?> vectorValues)) {
            throw new IllegalStateException("DashScope embedding vector is invalid");
        }
        float[] vector = new float[vectorValues.size()];
        for (int i = 0; i < vectorValues.size(); i++) {
            Object value = vectorValues.get(i);
            if (!(value instanceof Number number)) {
                throw new IllegalStateException("DashScope embedding vector contains non-number value");
            }
            vector[i] = number.floatValue();
        }
        return vector;
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://dashscope.aliyuncs.com";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
