package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.consistency.RagConsistencyService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.processing.DocumentProcessingService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.security.ChatResourceDeletionService;
import com.tyh.chat.validation.ChatFileValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeDocumentControllerTest {

    @Test
    void updateIgnoresServerManagedFieldsAndOnlyChangesTitle() throws Exception {
        KnowledgeDocumentService documentService = mock(KnowledgeDocumentService.class);
        ChatAccessService accessService = mock(ChatAccessService.class);
        KnowledgeDocument existing = new KnowledgeDocument();
        existing.setId("doc-1");
        existing.setKnowledgeBaseId("kb-1");
        existing.setUserId("100");
        existing.setFilePath("/profile/upload/original.pdf");
        existing.setStatus("READY");
        when(accessService.requireKnowledgeDocument("doc-1")).thenReturn(existing);
        when(accessService.currentUserId()).thenReturn("100");
        when(documentService.update(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        MockMvc mvc = mvc(documentService, accessService);

        mvc.perform(put("/ai/documents/doc-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"新标题","filePath":"../secret.txt","status":"FAILED","userId":"999"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<KnowledgeDocument> captor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(documentService).update(captor.capture());
        KnowledgeDocument update = captor.getValue();
        assertThat(update.getTitle()).isEqualTo("新标题");
        assertThat(update.getFilePath()).isNull();
        assertThat(update.getStatus()).isNull();
        assertThat(update.getUserId()).isEqualTo("100");
        assertThat(update.getKnowledgeBaseId()).isEqualTo("kb-1");
    }

    @Test
    void oldEmbeddingRetryRouteIsRemoved() throws Exception {
        MockMvc mvc = mvc(mock(KnowledgeDocumentService.class), mock(ChatAccessService.class));

        mvc.perform(post("/ai/documents/doc-1/embedding"))
                .andExpect(status().isNotFound());
    }

    private static MockMvc mvc(KnowledgeDocumentService documentService, ChatAccessService accessService) {
        KnowledgeDocumentController controller = new KnowledgeDocumentController(
                documentService,
                mock(DocumentProcessingService.class),
                mock(KnowledgeChunkService.class),
                accessService,
                mock(ChatResourceDeletionService.class),
                mock(ChatFileValidator.class),
                mock(RagConsistencyService.class));
        return MockMvcBuilders.standaloneSetup(controller).build();
    }
}
