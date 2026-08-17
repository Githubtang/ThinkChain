package com.tyh.chat.vendor.dashscope;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.chat.dto.ModelCallOptions;
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

    @Test
    void requestKeepsRolesSystemPromptAndModelOptions() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setSystemPrompt("你是项目助手");
        request.setMessages(List.of(
                message("user", "第一个问题"),
                message("assistant", "上一次回答"),
                message("user", "继续提问")));
        ModelCallOptions options = new ModelCallOptions();
        options.setTemperature(0.3D);
        options.setTopP(0.8D);
        options.setMaxTokens(2048);
        request.setOptions(options);

        MultiModalConversationParam param = new DashScopeSdkChatAdapter(new ObjectMapper())
                .buildParam(MODEL, request, true);

        List<String> roles = param.getMessages().stream()
                .map(MultiModalMessage.class::cast)
                .map(MultiModalMessage::getRole)
                .toList();
        assertThat(roles).containsExactly("system", "user", "assistant", "user");
        assertThat(param.getTemperature()).isEqualTo(0.3F);
        assertThat(param.getTopP()).isEqualTo(0.8D);
        assertThat(param.getMaxTokens()).isEqualTo(2048);
        assertThat(param.getIncrementalOutput()).isTrue();
    }

    private static Content content(String type, String url) {
        Content content = new Content();
        content.setType(type);
        content.setUrl(url);
        return content;
    }

    private static Message message(String role, String text) {
        Message message = new Message();
        message.setRole(role);
        Content content = new Content();
        content.setType("text");
        content.setText(text);
        message.setContents(List.of(content));
        return message;
    }
}
