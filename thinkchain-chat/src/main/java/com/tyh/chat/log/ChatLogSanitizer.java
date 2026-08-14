package com.tyh.chat.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 写入聊天日志前的序列化、敏感信息遮盖和长度控制组件。
 *
 * <p>它会把 apiKey、password、Authorization、token 等常见密钥值替换为 ***，
 * 并确保最终字符串不超过数据库字段长度。该组件是日志防护，不应被当作配置密钥的加密存储。</p>
 */
@Component
public class ChatLogSanitizer {

    private static final String TRUNCATED_SUFFIX = "...[TRUNCATED]";
    public static final int MAX_REQUEST_LENGTH = 65_535;
    public static final int MAX_RESPONSE_LENGTH = 65_535;
    public static final int MAX_ERROR_LENGTH = 2_000;

    private static final Pattern SECRET_JSON = Pattern.compile(
            "(?i)(\\\"(?:apiKey|api_key|password|authorization|token)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")");
    private static final Pattern AUTHORIZATION_TEXT = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*(?:bearer\\s+)?)[^\\s,;]+"
    );
    private static final Pattern SECRET_TEXT = Pattern.compile(
            "(?i)((?:api[_-]?key|password|token)\\s*[:=]\\s*)[^\\s,;]+"
    );

    private final ObjectMapper objectMapper;

    public ChatLogSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serializeRequest(Object request) {
        // 优先使用 JSON，结构化日志比 Java 对象默认 toString 更稳定、更容易排查。
        if (request == null) {
            return null;
        }
        try {
            return sanitizeAndTruncate(objectMapper.writeValueAsString(request), MAX_REQUEST_LENGTH);
        } catch (JsonProcessingException exception) {
            return truncate(String.valueOf(request), MAX_REQUEST_LENGTH);
        }
    }

    public String sanitizeResponse(String response) {
        return sanitizeAndTruncate(response, MAX_RESPONSE_LENGTH);
    }

    public String sanitizeError(String error) {
        return sanitizeAndTruncate(error, MAX_ERROR_LENGTH);
    }

    public String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        if (maxLength <= TRUNCATED_SUFFIX.length()) {
            return TRUNCATED_SUFFIX.substring(0, maxLength);
        }
        // 截断标记也计算在 maxLength 内，保证数据库不会因为标记额外字符而溢出。
        return value.substring(0, maxLength - TRUNCATED_SUFFIX.length()) + TRUNCATED_SUFFIX;
    }

    private String sanitizeAndTruncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String sanitized = SECRET_JSON.matcher(value).replaceAll("$1***$2");
        sanitized = AUTHORIZATION_TEXT.matcher(sanitized).replaceAll("$1***");
        sanitized = SECRET_TEXT.matcher(sanitized).replaceAll("$1***");
        return truncate(sanitized, maxLength);
    }
}
