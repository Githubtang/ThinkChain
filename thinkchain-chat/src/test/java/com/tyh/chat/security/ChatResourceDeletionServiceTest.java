package com.tyh.chat.security;

import com.tyh.chat.conversation.service.ConversationService;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import com.tyh.chat.rag.knowledge.service.KnowledgeBaseService;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatResourceDeletionServiceTest {

    @Test
    void deletesKnowledgeBaseChildrenBeforeParent() {
        ConversationService conversationService = mock(ConversationService.class);
        KnowledgeBaseService knowledgeBaseService = mock(KnowledgeBaseService.class);
        KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
        SessionDocumentService sessionDocumentService = mock(SessionDocumentService.class);
        KnowledgeChunkService chunkService = mock(KnowledgeChunkService.class);
        RagEmbeddingStore embeddingStore = mock(RagEmbeddingStore.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RagEmbeddingStore> provider = mock(ObjectProvider.class);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId("document-1");
        document.setKnowledgeBaseId("kb-1");
        document.setUserId("100");
        when(documentService.list(argThat(query -> "kb-1".equals(query.getKnowledgeBaseId())
                && "100".equals(query.getUserId())))).thenReturn(List.of(document));
        when(documentService.deleteById("document-1")).thenReturn(1);
        when(knowledgeBaseService.deleteById("kb-1")).thenReturn(1);
        when(provider.getIfAvailable()).thenReturn(embeddingStore);

        ChatResourceDeletionService service = new ChatResourceDeletionService(
                conversationService, knowledgeBaseService, documentService, sessionDocumentService,
                chunkService, provider);

        boolean deleted = service.deleteKnowledgeBase("kb-1", "100");

        assertThat(deleted).isTrue();
        InOrder order = inOrder(embeddingStore, chunkService, documentService, knowledgeBaseService);
        order.verify(embeddingStore).deleteByDocumentId("document-1");
        order.verify(chunkService).deleteByDocumentId("document-1");
        order.verify(documentService).deleteById("document-1");
        order.verify(knowledgeBaseService).deleteById("kb-1");
    }
}
