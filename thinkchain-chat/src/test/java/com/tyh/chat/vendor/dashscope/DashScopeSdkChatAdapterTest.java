package com.tyh.chat.vendor.dashscope;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.model.ModelEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DashScopeSdkChatAdapterTest {

    private static final ModelEntry MODEL = new ModelEntry(
            "qwen", "dashscope", "test-key",
            "https://workspace.example.com/compatible-mode/v1",
            "qwen-vl", Set.of("chat", "video", "audio"));

    @Test
    void videoUsesNativeVideoPartAndDoesNotInvokeAudioTranscription() throws Exception {
        DashScopeSdkChatAdapter adapter = new DashScopeSdkChatAdapter(new ObjectMapper());
        Content video = content("video", "https://cdn.example.com/demo.mp4");

        List<Map<String, Object>> parts = adapter.toDashScopeContentParts(MODEL, video);

        assertThat(parts).containsExactly(Map.of("video", "https://cdn.example.com/demo.mp4"));
    }

    @Test
    void audioIsTranscribedBeforeItBecomesChatText() throws Exception {
        DashScopeSdkChatAdapter adapter = new DashScopeSdkChatAdapter(new ObjectMapper()) {
            @Override
            String transcribeAudio(ModelEntry model, String audioUrl) {
                assertThat(model).isSameAs(MODEL);
                assertThat(audioUrl).isEqualTo("https://cdn.example.com/demo.mp3");
                return "这是音频中的内容";
            }
        };
        Content audio = content("audio", "https://cdn.example.com/demo.mp3");

        List<Map<String, Object>> parts = adapter.toDashScopeContentParts(MODEL, audio);

        assertThat(parts).containsExactly(Map.of("text", "[音频转写]\n这是音频中的内容"));
        assertThat(parts.getFirst()).doesNotContainKey("audio");
        assertThat(parts.getFirst()).doesNotContainKey("video");
    }

    @Test
    void paraformerResultExtractsTranscriptTextInOrder() throws Exception {
        String json = """
                {
                  "transcripts": [
                    {"channel_id": 0, "text": "第一段"},
                    {"channel_id": 1, "text": "第二段"}
                  ]
                }
                """;

        String transcript = DashScopeSdkChatAdapter.extractTranscript(
                new ObjectMapper().readTree(json));

        assertThat(transcript).isEqualTo("第一段\n第二段");
    }

    @Test
    void compatibleEndpointIsConvertedToNativeEndpoint() {
        String nativeBaseUrl = DashScopeSdkChatAdapter.nativeApiBaseUrl(
                "https://workspace.example.com/compatible-mode/v1");

        assertThat(nativeBaseUrl).isEqualTo("https://workspace.example.com/api/v1");
    }

    private static Content content(String type, String url) {
        Content content = new Content();
        content.setType(type);
        content.setUrl(url);
        return content;
    }
}
