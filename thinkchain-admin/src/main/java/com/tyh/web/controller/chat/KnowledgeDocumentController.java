package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI 知识文档")
@RestController
@RequestMapping("/ai")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;
    private final KnowledgeChunkService chunkService;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService,
                                       KnowledgeChunkService chunkService) {
        this.documentService = documentService;
        this.chunkService = chunkService;
    }

    @Operation(description = "创建文档元数据")
    @PostMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    public AjaxResult create(@PathVariable String knowledgeBaseId, @RequestBody KnowledgeDocument document) {
        document.setKnowledgeBaseId(knowledgeBaseId);
        return AjaxResult.success(documentService.create(document));
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
}
