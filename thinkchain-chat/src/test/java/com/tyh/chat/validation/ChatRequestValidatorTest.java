package com.tyh.chat.validation;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.chat.dto.ModelCallOptions;
import com.tyh.chat.rag.chat.dto.RagChatRequest;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.exception.ServiceException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatRequestValidatorTest {

    private final ChatRequestValidator requestValidator = new ChatRequestValidator();
    private final Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidTextChatRequest() {
        ChatRequest request = validTextRequest();

        assertThat(beanValidator.validate(request)).isEmpty();
        assertThatCode(() -> requestValidator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsRequestWithoutUserMessage() {
        ChatRequest request = validTextRequest();
        request.getMessages().get(0).setRole("assistant");

        assertThatThrownBy(() -> requestValidator.validate(request))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsNonTextContentWithoutTextOrUrl() {
        ChatRequest request = validTextRequest();
        request.getMessages().get(0).getContents().get(0).setType("image");
        request.getMessages().get(0).getContents().get(0).setText(null);

        assertThatThrownBy(() -> requestValidator.validate(request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("text或url");
    }

    @Test
    void validatesNestedModelOptions() {
        ChatRequest request = validTextRequest();
        ModelCallOptions options = new ModelCallOptions();
        options.setTemperature(3.0);
        options.setMaxTokens(0);
        request.setOptions(options);

        assertThat(beanValidator.validate(request)).hasSize(2);
    }

    @Test
    void kbOnlyRagRequiresKnowledgeBase() {
        RagChatRequest request = new RagChatRequest();
        request.setModel("qwen-plus");
        request.setQuestion("question");
        request.setRagMode("KB_ONLY");

        assertThatThrownBy(() -> requestValidator.validate(request))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("知识库");
    }

    private static ChatRequest validTextRequest() {
        Content content = new Content();
        content.setType("text");
        content.setText("hello");
        Message message = new Message();
        message.setRole("user");
        message.setContents(List.of(content));
        ChatRequest request = new ChatRequest();
        request.setModel("qwen-plus");
        request.setMessages(List.of(message));
        return request;
    }
}
