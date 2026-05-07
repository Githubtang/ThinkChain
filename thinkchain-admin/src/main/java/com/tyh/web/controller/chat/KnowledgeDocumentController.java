package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentParseService;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.embedding.service.RagEmbeddingService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import com.tyh.common.config.ThinkChainConfig;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.utils.file.FileUploadUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.ObjectProvider;
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

@Tag(name = "AI 知识文档")
@RestController
@RequestMapping("/ai")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;
    private final KnowledgeDocumentParseService parseService;
    private final RagEmbeddingService embeddingService;
    private final KnowledgeChunkService chunkService;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService,
                                       KnowledgeDocumentParseService parseService,
                                       RagEmbeddingService embeddingService,
                                       KnowledgeChunkService chunkService,
                                       ObjectProvider<RagEmbeddingStore> embeddingStoreProvider) {
        this.documentService = documentService;
        this.parseService = parseService;
        this.embeddingService = embeddingService;
        this.chunkService = chunkService;
        this.embeddingStoreProvider = embeddingStoreProvider;
    }

    @Operation(description = "创建文档元数据")
    @PostMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    public AjaxResult create(@PathVariable String knowledgeBaseId, @RequestBody KnowledgeDocument document) {
        document.setKnowledgeBaseId(knowledgeBaseId);
        return AjaxResult.success(documentService.create(document));
    }

    @Operation(description = "上传知识库文档")
    @PostMapping("/knowledge-bases/{knowledgeBaseId}/documents/upload")
    public AjaxResult upload(@PathVariable String knowledgeBaseId,
                             @RequestParam(required = false) String userId,
                             @RequestParam("file") MultipartFile file) {
        try {
            String fileName = FileUploadUtils.upload(ThinkChainConfig.getUploadPath(), file);
            KnowledgeDocument document = new KnowledgeDocument();
            document.setKnowledgeBaseId(knowledgeBaseId);
            document.setUserId(userId);
            document.setFileName(file.getOriginalFilename());
            document.setFileType(FilenameUtils.getExtension(file.getOriginalFilename()));
            document.setMimeType(file.getContentType());
            document.setFilePath(fileName);
            document.setFileSize(file.getSize());
            document.setTitle(file.getOriginalFilename());
            document.setStatus("UPLOADED");
            document.setChunkCount(0);
            documentService.create(document);
            return AjaxResult.success(parseService.parse(document.getId()));
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(description = "查询文档列表")
    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    public AjaxResult list(@PathVariable String knowledgeBaseId, KnowledgeDocument query) {
        query.setKnowledgeBaseId(knowledgeBaseId);
        return AjaxResult.success(documentService.list(query));
    }

    @Operation(description = "查询文档详情")
    @GetMapping("/documents/{documentId}")
    public AjaxResult get(@PathVariable String documentId) {
        return AjaxResult.success(documentService.getById(documentId));
    }

    @Operation(description = "更新文档")
    @PutMapping("/documents/{documentId}")
    public AjaxResult update(@PathVariable String documentId, @RequestBody KnowledgeDocument document) {
        document.setId(documentId);
        return AjaxResult.success(documentService.update(document));
    }

    @Operation(description = "删除文档")
    @DeleteMapping("/documents/{documentId}")
    public AjaxResult delete(@PathVariable String documentId) {
        embeddingStoreProvider.ifAvailable(store -> store.deleteByDocumentId(documentId));
        chunkService.deleteByDocumentId(documentId);
        return AjaxResult.success(documentService.deleteById(documentId));
    }

    @Operation(description = "查询文档切片列表")
    @GetMapping("/documents/{documentId}/chunks")
    public AjaxResult chunks(@PathVariable String documentId) {
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        return AjaxResult.success(chunkService.list(query));
    }

    @Operation(description = "解析知识库文档")
    @PostMapping("/documents/{documentId}/parse")
    public AjaxResult parse(@PathVariable String documentId) {
        return AjaxResult.success(parseService.parse(documentId));
    }

    @Operation(description = "向量化知识库文档切片")
    @PostMapping("/documents/{documentId}/embedding")
    public AjaxResult embedding(@PathVariable String documentId) {
        return AjaxResult.success(embeddingService.embedDocument(documentId));
    }
}
