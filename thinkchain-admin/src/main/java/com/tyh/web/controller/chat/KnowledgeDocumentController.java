package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.processing.DocumentProcessingService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.security.ChatResourceDeletionService;
import com.tyh.chat.validation.ChatFileValidator;
import com.tyh.common.core.domain.AjaxResult;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    public KnowledgeDocumentController(KnowledgeDocumentService documentService,
                                       DocumentProcessingService processingService,
                                       KnowledgeChunkService chunkService,
                                       ChatAccessService accessService,
                                       ChatResourceDeletionService deletionService,
                                       ChatFileValidator fileValidator) {
        this.documentService = documentService;
        this.processingService = processingService;
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

    @Operation(summary = "上传知识库文档",
            description = "支持文本、pdf、doc、docx、xls、xlsx、ppt、pptx；上传成功后在后台解析、切片和向量化")
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
    public TableDataInfo chunks(@PathVariable String documentId) {
        accessService.requireKnowledgeDocument(documentId);
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        startPage();
        return getDataTable(chunkService.list(query));
    }

    @Operation(summary = "重新处理知识库文档", description = "后台重新执行解析、切片和向量化")
    @PostMapping("/documents/{documentId}/parse")
    public AjaxResult parse(@PathVariable String documentId) {
        accessService.requireKnowledgeDocument(documentId);
        boolean accepted = processingService.retryKnowledgeDocument(documentId);
        AjaxResult result = AjaxResult.success(documentService.getById(documentId));
        result.put("processingAccepted", accepted);
        return result;
    }

    @Operation(summary = "重试知识库文档处理", description = "兼容原 embedding 重试地址，后台重新执行完整处理链路")
    @PostMapping("/documents/{documentId}/embedding")
    public AjaxResult embedding(@PathVariable String documentId) {
        accessService.requireKnowledgeDocument(documentId);
        boolean accepted = processingService.retryKnowledgeDocument(documentId);
        AjaxResult result = AjaxResult.success(documentService.getById(documentId));
        result.put("processingAccepted", accepted);
        return result;
    }
}
