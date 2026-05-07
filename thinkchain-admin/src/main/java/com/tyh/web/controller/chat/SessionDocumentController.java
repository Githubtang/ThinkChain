package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentParseService;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.common.config.ThinkChainConfig;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.utils.file.FileUploadUtils;
import com.tyh.framework.config.ServerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI Session Document")
@RestController
@RequestMapping("/ai/chat")
public class SessionDocumentController {

    private final SessionDocumentService documentService;
    private final SessionDocumentParseService parseService;
    private final KnowledgeChunkService chunkService;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;
    private final ServerConfig serverConfig;

    public SessionDocumentController(SessionDocumentService documentService,
                                     SessionDocumentParseService parseService,
                                     KnowledgeChunkService chunkService,
                                     ObjectProvider<RagEmbeddingStore> embeddingStoreProvider,
                                     ServerConfig serverConfig) {
        this.documentService = documentService;
        this.parseService = parseService;
        this.chunkService = chunkService;
        this.embeddingStoreProvider = embeddingStoreProvider;
        this.serverConfig = serverConfig;
    }

    @Operation(description = "Upload session document")
    @PostMapping("/conversations/{conversationId}/documents")
    public AjaxResult upload(@PathVariable String conversationId,
                             @RequestParam(required = false) String userId,
                             @RequestParam("file") MultipartFile file) {
        try {
            String fileName = FileUploadUtils.upload(ThinkChainConfig.getUploadPath(), file);
            SessionDocument document = new SessionDocument();
            document.setConversationId(conversationId);
            document.setUserId(userId);
            document.setFileName(fileName);
            document.setOriginalFileName(file.getOriginalFilename());
            document.setFileUrl(serverConfig.getUrl() + fileName);
            document.setFilePath(fileName);
            document.setMimeType(file.getContentType());
            document.setFileSize(file.getSize());
            document.setParseStatus("PENDING");
            document.setChunkCount(0);
            documentService.create(document);
            return AjaxResult.success(parseService.parse(document.getId()));
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(description = "List session documents")
    @GetMapping("/conversations/{conversationId}/documents")
    public AjaxResult list(@PathVariable String conversationId,
                           @RequestParam(required = false) String userId) {
        SessionDocument query = new SessionDocument();
        query.setConversationId(conversationId);
        query.setUserId(userId);
        return AjaxResult.success(documentService.list(query));
    }

    @Operation(description = "Get session document")
    @GetMapping("/session-documents/{documentId}")
    public AjaxResult get(@PathVariable String documentId) {
        return AjaxResult.success(documentService.getById(documentId));
    }

    @Operation(description = "List session document chunks")
    @GetMapping("/session-documents/{documentId}/chunks")
    public AjaxResult chunks(@PathVariable String documentId) {
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        query.setScopeType("SESSION");
        return AjaxResult.success(chunkService.list(query));
    }

    @Operation(description = "Delete session document")
    @DeleteMapping("/session-documents/{documentId}")
    public AjaxResult delete(@PathVariable String documentId) {
        embeddingStoreProvider.ifAvailable(store -> store.deleteByDocumentId(documentId));
        chunkService.deleteByDocumentId(documentId);
        return AjaxResult.success(documentService.deleteById(documentId));
    }
}
