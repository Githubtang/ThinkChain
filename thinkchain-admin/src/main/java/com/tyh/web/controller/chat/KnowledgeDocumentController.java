package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentParseService;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.embedding.service.RagEmbeddingService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.security.ChatResourceDeletionService;
import com.tyh.chat.validation.ChatFileValidator;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.exception.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档的上传、查询、解析、切片查看和向量化接口。
 *
 * <p>上传完成后会依次执行解析、切片和向量化；embedding 接口保留为失败重试入口。
 * 当前仅支持文本类扩展名，不支持 PDF、Word。</p>
 */
@Tag(name = "AI 知识文档")
@RestController
@RequestMapping("/ai")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;
    private final KnowledgeDocumentParseService parseService;
    private final RagEmbeddingService embeddingService;
    private final KnowledgeChunkService chunkService;
    private final ChatAccessService accessService;
    private final ChatResourceDeletionService deletionService;
    private final ChatFileValidator fileValidator;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService,
                                       KnowledgeDocumentParseService parseService,
                                       RagEmbeddingService embeddingService,
                                       KnowledgeChunkService chunkService,
                                       ChatAccessService accessService,
                                       ChatResourceDeletionService deletionService,
                                       ChatFileValidator fileValidator) {
        this.documentService = documentService;
        this.parseService = parseService;
        this.embeddingService = embeddingService;
        this.chunkService = chunkService;
        this.accessService = accessService;
        this.deletionService = deletionService;
        this.fileValidator = fileValidator;
    }

    @Operation(summary = "创建文档元数据", description = "创建文档元数据")
    @PostMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    public AjaxResult create(@PathVariable String knowledgeBaseId, @Valid @RequestBody KnowledgeDocument document) {
        accessService.requireKnowledgeBase(knowledgeBaseId);
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setUserId(accessService.currentUserId());
        documentService.create(document);
        return AjaxResult.success(document);
    }

    @Operation(summary = "上传知识库文档", description = "上传知识库文档")
    @PostMapping("/knowledge-bases/{knowledgeBaseId}/documents/upload")
    public AjaxResult upload(@PathVariable String knowledgeBaseId,
                             @RequestParam("file") MultipartFile file) {
        // 先确认知识库属于当前用户，再校验并保存物理文件，最后创建元数据和解析切片。
        accessService.requireKnowledgeBase(knowledgeBaseId);
        String fileName = fileValidator.upload(file);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setUserId(accessService.currentUserId());
        document.setFileName(file.getOriginalFilename());
        document.setFileType(FilenameUtils.getExtension(file.getOriginalFilename()));
        document.setMimeType(file.getContentType());
        document.setFilePath(fileName);
        document.setFileSize(file.getSize());
        document.setTitle(file.getOriginalFilename());
        document.setStatus("UPLOADED");
        document.setChunkCount(0);
        documentService.create(document);
        KnowledgeDocument parsed = parseService.parse(document.getId());
        if (parsed != null && "FAILED".equals(parsed.getStatus())) {
            throw new ServiceException("文档解析失败", HttpStatus.BAD_REQUEST)
                    .setDetailMessage(parsed.getErrorMessage());
        }
        embedAndUpdateStatus(parsed);
        return AjaxResult.success(documentService.getById(document.getId()));
    }

    @Operation(summary = "查询文档列表", description = "查询文档列表")
    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    public AjaxResult list(@PathVariable String knowledgeBaseId, KnowledgeDocument query) {
        accessService.requireKnowledgeBase(knowledgeBaseId);
        query.setKnowledgeBaseId(knowledgeBaseId);
        query.setUserId(accessService.currentUserId());
        return AjaxResult.success(documentService.list(query));
    }

    @Operation(summary = "查询文档详情", description = "查询文档详情")
    @GetMapping("/documents/{documentId}")
    public AjaxResult get(@PathVariable String documentId) {
        return AjaxResult.success(accessService.requireKnowledgeDocument(documentId));
    }

    @Operation(summary = "更新文档", description = "更新文档")
    @PutMapping("/documents/{documentId}")
    public AjaxResult update(@PathVariable String documentId, @Valid @RequestBody KnowledgeDocument document) {
        KnowledgeDocument existing = accessService.requireKnowledgeDocument(documentId);
        if (document.getKnowledgeBaseId() != null) {
            accessService.requireKnowledgeBase(document.getKnowledgeBaseId());
        } else {
            document.setKnowledgeBaseId(existing.getKnowledgeBaseId());
        }
        document.setId(documentId);
        document.setUserId(accessService.currentUserId());
        return AjaxResult.success(documentService.update(document) > 0);
    }

    @Operation(summary = "删除文档", description = "删除文档")
    @DeleteMapping("/documents/{documentId}")
    public AjaxResult delete(@PathVariable String documentId) {
        KnowledgeDocument document = accessService.requireKnowledgeDocument(documentId);
        return AjaxResult.success(deletionService.deleteKnowledgeDocument(document));
    }

    @Operation(summary = "查询文档切片列表", description = "查询文档切片列表")
    @GetMapping("/documents/{documentId}/chunks")
    public AjaxResult chunks(@PathVariable String documentId) {
        accessService.requireKnowledgeDocument(documentId);
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        return AjaxResult.success(chunkService.list(query));
    }

    @Operation(summary = "解析知识库文档", description = "解析知识库文档")
    @PostMapping("/documents/{documentId}/parse")
    public AjaxResult parse(@PathVariable String documentId) {
        accessService.requireKnowledgeDocument(documentId);
        KnowledgeDocument parsed = parseService.parse(documentId);
        if (parsed != null && "FAILED".equals(parsed.getStatus())) {
            throw new ServiceException("文档解析失败", HttpStatus.BAD_REQUEST)
                    .setDetailMessage(parsed.getErrorMessage());
        }
        return AjaxResult.success(parsed);
    }

    @Operation(summary = "向量化知识库文档切片", description = "向量化知识库文档切片")
    @PostMapping("/documents/{documentId}/embedding")
    public AjaxResult embedding(@PathVariable String documentId) {
        // 向量化前再次检查文档归属，防止用户通过猜测 documentId 处理他人资料。
        KnowledgeDocument document = accessService.requireKnowledgeDocument(documentId);
        return AjaxResult.success(embedAndUpdateStatus(document));
    }

    /** 执行向量化并根据剩余失败切片更新文档总体状态。 */
    private int embedAndUpdateStatus(KnowledgeDocument document) {
        document.setStatus("EMBEDDING");
        document.setErrorMessage(null);
        documentService.update(document);
        try {
            int successCount = embeddingService.embedDocument(document.getId());
            boolean hasRemaining = hasChunksWithStatus(document.getId(), "PENDING")
                    || hasChunksWithStatus(document.getId(), "FAILED");
            document.setStatus(hasRemaining ? "FAILED" : "READY");
            document.setErrorMessage(hasRemaining ? "部分切片向量化失败，可重新调用 embedding 接口重试" : null);
            documentService.update(document);
            return successCount;
        } catch (Exception exception) {
            document.setStatus("FAILED");
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
