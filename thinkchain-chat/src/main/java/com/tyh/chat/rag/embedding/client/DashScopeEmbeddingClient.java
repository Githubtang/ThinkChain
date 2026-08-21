package com.tyh.chat.rag.embedding.client;

import com.tyh.chat.rag.embedding.config.RagEmbeddingProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
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
    private final RestTemplate restTemplate;

    public DashScopeEmbeddingClient(RagEmbeddingProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder
                .connectTimeout(Duration.ofMillis(Math.max(1, properties.getConnectTimeoutMs())))
                .readTimeout(Duration.ofMillis(Math.max(1, properties.getReadTimeoutMs())))
                .build();
    }

    /** 测试时允许传入绑定 MockRestServiceServer 的 RestTemplate。 */
    DashScopeEmbeddingClient(RagEmbeddingProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
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
        int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.postForObject(
                        url, new HttpEntity<>(body, headers), Map.class);
                float[] vector = extractEmbedding(response);
                int expectedDimensions = properties.getDimensions() != null ? properties.getDimensions() : 0;
                if (expectedDimensions > 0 && vector.length != expectedDimensions) {
                    throw new IllegalStateException("DashScope embedding dimensions mismatch: expected "
                            + expectedDimensions + ", actual " + vector.length);
                }
                return vector;
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                if (attempt == maxAttempts || (status != 429 && status < 500)) {
                    throw exception;
                }
                waitBeforeRetry(attempt);
            } catch (ResourceAccessException exception) {
                if (attempt == maxAttempts) {
                    throw exception;
                }
                waitBeforeRetry(attempt);
            }
        }
        throw new IllegalStateException("DashScope embedding request failed");
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

    /** 对临时网络错误做短暂退避；线程被取消时立即停止，不吞掉中断信号。 */
    private void waitBeforeRetry(int attempt) {
        try {
            Thread.sleep(Math.max(0L, properties.getRetryDelayMs()) * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding retry interrupted", exception);
        }
    }
}
