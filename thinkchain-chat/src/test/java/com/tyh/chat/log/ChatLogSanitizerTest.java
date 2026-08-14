package com.tyh.chat.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChatLogSanitizerTest {

    private final ChatLogSanitizer sanitizer = new ChatLogSanitizer(new ObjectMapper());

    @Test
    void redactsJsonAndPlainTextSecrets() {
        String json = sanitizer.serializeRequest(Map.of("apiKey", "secret-one", "token", "secret-two"));
        String text = sanitizer.sanitizeError(
                "Authorization: Bearer secret-three; password=secret-four api_key=secret-five");

        assertThat(json).contains("***").doesNotContain("secret-one", "secret-two");
        assertThat(text).contains("***").doesNotContain("secret-three", "secret-four", "secret-five");
    }

    @Test
    void truncatesOversizedValues() {
        String value = "x".repeat(ChatLogSanitizer.MAX_ERROR_LENGTH + 10);

        String result = sanitizer.sanitizeError(value);

        assertThat(result).endsWith("...[TRUNCATED]");
        assertThat(result).hasSize(ChatLogSanitizer.MAX_ERROR_LENGTH);
    }
}
