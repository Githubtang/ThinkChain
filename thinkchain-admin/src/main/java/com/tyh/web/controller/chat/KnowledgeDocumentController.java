package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.dto.KnowledgeDocumentUpdateRequest;
import com.tyh.chat.rag.consistency.RagConsistencyService;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.processing.DocumentProcessingService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.security.ChatResourceDeletionService;
import com.tyh.chat.validation.ChatFileValidator;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.annotation.RateLimiter;
import com.tyh.common.annotation.RepeatSubmit;
import com.tyh.common.enums.LimitType;
import com.tyh.common.core.controller.BaseController;
import com.tyh.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.commons.io.FilenameUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库文档的上传、查询、解析、切片查看和向量化接口。
 *
 * <p>上传接口只保存文件和元数据，然后提交后台任务；解析、切片和向量化不会阻塞 HTTP 请求。
 * 客户端通过详情接口读取 UPLOADED/PARSING/CHUNKING/EMBEDDING/READY/FAILED 状态。</p>
 */
@Tag(name = "AI 知识文档")
@RestController
@RequestMapping("/ai")
public class KnowledgeDocumentController extends BaseController {

    private final KnowledgeDocumentService documentService;
    private final DocumentProcessingService processingService;
    private final KnowledgeChunkService chunkService;
    private final ChatAccessService accessService;
    private final ChatResourceDeletionService deletionService;
    private final ChatFileValidator fileValidator;
    private final RagConsistencyService consistencyService;

    public KnowledgeDocumentController(KnowledgeDocumentService documentService,
                                       DocumentProcessingService processingService,
                                       KnowledgeChunkService chunkService,
                                       ChatAccessService accessService,
                                       ChatResourceDeletionService deletionService,
                                       ChatFileValidator fileValidator,
                                       RagConsistencyService consistencyService) {
        this.documentService = documentService;
        this.processingService = processingService;
        this.chunkService = chunkService;
        this.accessService = accessService;
        this.deletionService = deletionService;
        this.fileValidator = fileValidator;
        this.consistencyService = consistencyService;
    }

    @Operation(summary = "上传知识库文档",
            description = "支持文本、pdf、doc、docx、xls、xlsx、ppt、pptx；上传成功后在后台解析、切片和向量化")
    @PostMapping("/knowledge-bases/{knowledgeBaseId}/documents/upload")
    @RateLimiter(key = "ai:document:upload:", time = 60, count = 10, limitType = LimitType.USER)
    @RepeatSubmit(interval = 3000, message = "文档正在上传，请勿重复提交")
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
        boolean accepted = processingService.submitKnowledgeDocument(document.getId());
        AjaxResult result = AjaxResult.success(documentService.getById(document.getId()));
        result.put("processingAccepted", accepted);
        return result;
    }

    @Operation(summary = "查询文档列表", description = "查询文档列表")
    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    public TableDataInfo list(@PathVariable String knowledgeBaseId, KnowledgeDocument query) {
        accessService.requireKnowledgeBase(knowledgeBaseId);
        query.setKnowledgeBaseId(knowledgeBaseId);
        query.setUserId(accessService.currentUserId());
        startPage();
        return getDataTable(documentService.list(query));
    }

    @Operation(summary = "查询文档详情", description = "查询文档详情")
    @GetMapping("/documents/{documentId}")
    public AjaxResult get(@PathVariable String documentId) {
        return AjaxResult.success(accessService.requireKnowledgeDocument(documentId));
    }

    @Operation(summary = "更新文档", description = "更新文档")
    @PutMapping("/documents/{documentId}")
    public AjaxResult update(@PathVariable String documentId,
                             @Valid @RequestBody KnowledgeDocumentUpdateRequest request) {
        KnowledgeDocument existing = accessService.requireKnowledgeDocument(documentId);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(documentId);
        document.setKnowledgeBaseId(existing.getKnowledgeBaseId());
        document.setUserId(accessService.currentUserId());
        document.setTitle(request.getTitle());
        // 标题更新不能顺带清除后台处理失败原因。
        document.setErrorMessage(existing.getErrorMessage());
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
    public TableDataInfo chunks(@PathVariable String documentId) {
        accessService.requireKnowledgeDocument(documentId);
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        startPage();
        return getDataTable(chunkService.list(query));
    }

    @Operation(summary = "重试知识库文档处理", description = "后台重新执行解析、切片和向量化")
    @PostMapping("/documents/{documentId}/retry")
    @RateLimiter(key = "ai:document:retry:", time = 60, count = 10, limitType = LimitType.USER)
    @RepeatSubmit(interval = 3000, message = "文档重试任务已提交，请勿重复操作")
    public AjaxResult retry(@PathVariable String documentId) {
        accessService.requireKnowledgeDocument(documentId);
        boolean accepted = processingService.retryKnowledgeDocument(documentId);
        AjaxResult result = AjaxResult.success(documentService.getById(documentId));
        result.put("processingAccepted", accepted);
        return result;
    }

    @Operation(summary = "检查知识库文档向量一致性", description = "比较 MySQL 切片与 Supabase 向量记录")
    @GetMapping("/documents/{documentId}/consistency")
    public AjaxResult consistency(@PathVariable String documentId) {
        accessService.requireKnowledgeDocument(documentId);
        return AjaxResult.success(consistencyService.inspect(documentId));
    }

    @Operation(summary = "修复知识库文档向量一致性", description = "删除孤立向量并补写缺失向量")
    @PostMapping("/documents/{documentId}/consistency/repair")
    @RateLimiter(key = "ai:document:repair:", time = 60, count = 5, limitType = LimitType.USER)
    @RepeatSubmit(interval = 5000, message = "一致性修复正在执行，请勿重复操作")
    public AjaxResult repairConsistency(@PathVariable String documentId) {
        accessService.requireKnowledgeDocument(documentId);
        return AjaxResult.success(consistencyService.repair(documentId));
    }
}
