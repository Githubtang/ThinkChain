package com.tyh.chat.security;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.conversation.domain.ChatConversation;
import com.tyh.chat.conversation.service.ConversationService;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.knowledge.service.KnowledgeBaseService;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatAccessServiceTest {

    private final CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
    private final ConversationService conversationService = mock(ConversationService.class);
    private final KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
    private final KnowledgeDocumentService knowledgeDocumentService = mock(KnowledgeDocumentService.class);
    private final SessionDocumentService sessionDocumentService = mock(SessionDocumentService.class);

    private ChatAccessService accessService;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.currentUserId()).thenReturn("100");
        accessService = new ChatAccessService(currentUserProvider, conversationService, knowledgeBaseService,
                knowledgeDocumentService, sessionDocumentService);
    }

    @Test
    void prepareAlwaysOverridesClientSuppliedUserId() {
        ChatRequest request = new ChatRequest();
        request.setUserId("spoofed-user");

        accessService.prepare(request);

        assertThat(request.getUserId()).isEqualTo("100");
    }

    @Test
    void rejectsForeignConversation() {
        ChatConversation conversation = new ChatConversation();
        conversation.setId("conversation-1");
        conversation.setUserId("200");
        when(conversationService.getConversation("conversation-1")).thenReturn(conversation);

        assertThatThrownBy(() -> accessService.requireConversation("conversation-1"))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectsMissingConversation() {
        when(conversationService.getConversation("missing")).thenReturn(null);

        assertThatThrownBy(() -> accessService.requireConversation("missing"))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectsSessionDocumentFromAnotherConversation() {
        SessionDocument document = new SessionDocument();
        document.setId("document-1");
        document.setUserId("100");
        document.setConversationId("conversation-2");
        when(sessionDocumentService.getById("document-1")).thenReturn(document);

        ChatRequest request = new ChatRequest();
        request.setConversationId("conversation-1");
        request.setSessionDocumentIds(List.of("document-1"));
        ChatConversation conversation = new ChatConversation();
        conversation.setId("conversation-1");
        conversation.setUserId("100");
        when(conversationService.getConversation("conversation-1")).thenReturn(conversation);

        assertThatThrownBy(() -> accessService.prepare(request))
                .isInstanceOf(ServiceException.class)
                .extracting(exception -> ((ServiceException) exception).getCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
