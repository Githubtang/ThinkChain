package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.embedding.service.RagEmbeddingService;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentParseService;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.security.ChatResourceDeletionService;
import com.tyh.chat.validation.ChatFileValidator;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.exception.ServiceException;
import com.tyh.framework.config.ServerConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 当前会话临时文档接口。
 *
 * <p>这类文档只服务于一个 conversationId，不进入长期知识库。上传后会自动完成解析、切片和向量化；
 * RAG 请求使用会话文档时，后端会同时检查用户归属和会话归属。</p>
 */
@Tag(name = "AI 会话文档")
@RestController
@RequestMapping("/ai/chat")
public class SessionDocumentController {

    private final SessionDocumentService documentService;
    private final SessionDocumentParseService parseService;
    private final RagEmbeddingService embeddingService;
    private final KnowledgeChunkService chunkService;
    private final ServerConfig serverConfig;
    private final ChatAccessService accessService;
    private final ChatResourceDeletionService deletionService;
    private final ChatFileValidator fileValidator;

    public SessionDocumentController(SessionDocumentService documentService,
                                     SessionDocumentParseService parseService,
                                     RagEmbeddingService embeddingService,
                                     KnowledgeChunkService chunkService,
                                     ServerConfig serverConfig,
                                     ChatAccessService accessService,
                                     ChatResourceDeletionService deletionService,
                                     ChatFileValidator fileValidator) {
        this.documentService = documentService;
        this.parseService = parseService;
        this.embeddingService = embeddingService;
        this.chunkService = chunkService;
        this.serverConfig = serverConfig;
        this.accessService = accessService;
        this.deletionService = deletionService;
        this.fileValidator = fileValidator;
    }

    @Operation(summary = "上传会话文档", description = "上传会话文档")
    @PostMapping("/conversations/{conversationId}/documents")
    public AjaxResult upload(@PathVariable String conversationId,
                             @RequestParam("file") MultipartFile file) {
        // 会话归属通过后才允许写文件和创建文档元数据。
        accessService.requireConversation(conversationId);
        String fileName = fileValidator.upload(file);
        SessionDocument document = new SessionDocument();
        document.setConversationId(conversationId);
        document.setUserId(accessService.currentUserId());
        document.setFileName(fileName);
        document.setOriginalFileName(file.getOriginalFilename());
        document.setFileUrl(serverConfig.getUrl() + fileName);
        document.setFilePath(fileName);
        document.setMimeType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setParseStatus("PENDING");
        document.setChunkCount(0);
        documentService.create(document);
        SessionDocument parsed = parseService.parse(document.getId());
        if (parsed != null && "FAILED".equals(parsed.getParseStatus())) {
            throw new ServiceException("文档解析失败", HttpStatus.BAD_REQUEST)
                    .setDetailMessage(parsed.getErrorMessage());
        }
        embedAndUpdateStatus(parsed);
        return AjaxResult.success(documentService.getById(document.getId()));
    }

    @Operation(summary = "查询会话文档列表", description = "查询会话文档列表")
    @GetMapping("/conversations/{conversationId}/documents")
    public AjaxResult list(@PathVariable String conversationId,
                           @RequestParam(required = false) String parseStatus) {
        accessService.requireConversation(conversationId);
        SessionDocument query = new SessionDocument();
        query.setConversationId(conversationId);
        query.setUserId(accessService.currentUserId());
        query.setParseStatus(parseStatus);
        return AjaxResult.success(documentService.list(query));
    }

    @Operation(summary = "查询会话文档详情", description = "查询会话文档详情")
    @GetMapping("/session-documents/{documentId}")
    public AjaxResult get(@PathVariable String documentId) {
        return AjaxResult.success(accessService.requireSessionDocument(documentId));
    }

    @Operation(summary = "查询会话文档切片列表", description = "查询会话文档切片列表")
    @GetMapping("/session-documents/{documentId}/chunks")
    public AjaxResult chunks(@PathVariable String documentId) {
        accessService.requireSessionDocument(documentId);
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        query.setScopeType("SESSION");
        return AjaxResult.success(chunkService.list(query));
    }

    @Operation(summary = "向量化会话文档切片", description = "向量化会话文档切片")
    @PostMapping("/session-documents/{documentId}/embedding")
    public AjaxResult embedding(@PathVariable String documentId) {
        SessionDocument document = accessService.requireSessionDocument(documentId);
        return AjaxResult.success(embedAndUpdateStatus(document));
    }

    @Operation(summary = "删除会话文档", description = "删除会话文档")
    @DeleteMapping("/session-documents/{documentId}")
    public AjaxResult delete(@PathVariable String documentId) {
        // 删除服务统一清理 Supabase 向量、主库切片/元数据和磁盘文件。
        SessionDocument document = accessService.requireSessionDocument(documentId);
        return AjaxResult.success(deletionService.deleteSessionDocument(document));
    }

    /** 执行向量化并根据剩余失败切片更新会话文档总体状态。 */
    private int embedAndUpdateStatus(SessionDocument document) {
        document.setParseStatus("EMBEDDING");
        document.setErrorMessage(null);
        documentService.update(document);
        try {
            int successCount = embeddingService.embedDocument(document.getId());
            boolean hasRemaining = hasChunksWithStatus(document.getId(), "PENDING")
                    || hasChunksWithStatus(document.getId(), "FAILED");
            document.setParseStatus(hasRemaining ? "FAILED" : "READY");
            document.setErrorMessage(hasRemaining ? "部分切片向量化失败，可重新调用 embedding 接口重试" : null);
            documentService.update(document);
            return successCount;
        } catch (Exception exception) {
            document.setParseStatus("FAILED");
            document.setErrorMessage(exception.getMessage());
            documentService.update(document);
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new ServiceException("文档向量化失败", HttpStatus.ERROR)
                    .setDetailMessage(exception.getMessage());
        }
    }

    /** 判断文档是否还有指定状态的切片。 */
    private boolean hasChunksWithStatus(String documentId, String status) {
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        query.setEmbeddingStatus(status);
        return !chunkService.list(query).isEmpty();
    }
}
