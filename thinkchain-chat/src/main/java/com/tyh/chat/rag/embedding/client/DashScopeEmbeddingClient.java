package com.tyh.chat.rag.embedding.client;

import com.tyh.chat.rag.embedding.config.RagEmbeddingProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 阿里云 DashScope 文本向量接口实现。
 *
 * <p>本类只负责 HTTP 协议转换：读取配置、设置 Bearer 密钥、发送文本并把厂商 JSON 中的数字列表
 * 转为 float[]。它不负责切片、保存数据库或相似度检索。</p>
 */
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
        // 提前检查密钥，可以给出比远端 401 更容易理解的配置错误。
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("DashScope embedding apiKey is blank");
        }
        String url = trimTrailingSlash(properties.getBaseUrl()) + EMBEDDING_PATH;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        // dimensions 必须与数据库 rag_embedding.embedding 的 vector(n) 维度一致。
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
        // 厂商返回采用多层 Map/List 结构，逐层检查可避免格式变化时出现难理解的类型转换异常。
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
