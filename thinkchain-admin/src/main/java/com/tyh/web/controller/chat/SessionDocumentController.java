package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.processing.DocumentProcessingService;
import com.tyh.chat.rag.consistency.RagConsistencyService;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.security.ChatResourceDeletionService;
import com.tyh.chat.validation.ChatFileValidator;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.annotation.RateLimiter;
import com.tyh.common.annotation.RepeatSubmit;
import com.tyh.common.enums.LimitType;
import com.tyh.common.core.controller.BaseController;
import com.tyh.common.core.page.TableDataInfo;
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
 * <p>这类文档只服务于一个 conversationId，不进入长期知识库。上传后由后台完成解析、切片和向量化；
 * 支持文本、PDF、Word、Excel 和 PowerPoint。RAG 请求使用会话文档时，后端会同时检查用户归属和会话归属。</p>
 */
@Tag(name = "AI 会话文档")
@RestController
@RequestMapping("/ai/chat")
public class SessionDocumentController extends BaseController {

    private final SessionDocumentService documentService;
    private final DocumentProcessingService processingService;
    private final KnowledgeChunkService chunkService;
    private final ServerConfig serverConfig;
    private final ChatAccessService accessService;
    private final ChatResourceDeletionService deletionService;
    private final ChatFileValidator fileValidator;
    private final RagConsistencyService consistencyService;

    public SessionDocumentController(SessionDocumentService documentService,
                                     DocumentProcessingService processingService,
                                     KnowledgeChunkService chunkService,
                                     ServerConfig serverConfig,
                                     ChatAccessService accessService,
                                     ChatResourceDeletionService deletionService,
                                     ChatFileValidator fileValidator,
                                     RagConsistencyService consistencyService) {
        this.documentService = documentService;
        this.processingService = processingService;
        this.chunkService = chunkService;
        this.serverConfig = serverConfig;
        this.accessService = accessService;
        this.deletionService = deletionService;
        this.fileValidator = fileValidator;
        this.consistencyService = consistencyService;
    }

    @Operation(summary = "上传会话文档",
            description = "支持文本、pdf、doc、docx、xls、xlsx、ppt、pptx；上传成功后在后台解析、切片和向量化")
    @PostMapping("/conversations/{conversationId}/documents")
    @RateLimiter(key = "ai:session-document:upload:", time = 60, count = 10, limitType = LimitType.USER)
    @RepeatSubmit(interval = 3000, message = "文档正在上传，请勿重复提交")
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
        boolean accepted = processingService.submitSessionDocument(document.getId());
        AjaxResult result = AjaxResult.success(documentService.getById(document.getId()));
        result.put("processingAccepted", accepted);
        return result;
    }

    @Operation(summary = "查询会话文档列表", description = "查询会话文档列表")
    @GetMapping("/conversations/{conversationId}/documents")
    public TableDataInfo list(@PathVariable String conversationId,
                              @RequestParam(required = false) String parseStatus) {
        accessService.requireConversation(conversationId);
        SessionDocument query = new SessionDocument();
        query.setConversationId(conversationId);
        query.setUserId(accessService.currentUserId());
        query.setParseStatus(parseStatus);
        startPage();
        return getDataTable(documentService.list(query));
    }

    @Operation(summary = "查询会话文档详情", description = "查询会话文档详情")
    @GetMapping("/session-documents/{documentId}")
    public AjaxResult get(@PathVariable String documentId) {
        return AjaxResult.success(accessService.requireSessionDocument(documentId));
    }

    @Operation(summary = "查询会话文档切片列表", description = "查询会话文档切片列表")
    @GetMapping("/session-documents/{documentId}/chunks")
    public TableDataInfo chunks(@PathVariable String documentId) {
        accessService.requireSessionDocument(documentId);
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        query.setScopeType("SESSION");
        startPage();
        return getDataTable(chunkService.list(query));
    }

    @Operation(summary = "重试会话文档处理", description = "后台重新执行解析、切片和向量化")
    @PostMapping("/session-documents/{documentId}/retry")
    @RateLimiter(key = "ai:session-document:retry:", time = 60, count = 10, limitType = LimitType.USER)
    @RepeatSubmit(interval = 3000, message = "文档重试任务已提交，请勿重复操作")
    public AjaxResult retry(@PathVariable String documentId) {
        accessService.requireSessionDocument(documentId);
        boolean accepted = processingService.retrySessionDocument(documentId);
        AjaxResult result = AjaxResult.success(documentService.getById(documentId));
        result.put("processingAccepted", accepted);
        return result;
    }

    @Operation(summary = "检查会话文档向量一致性", description = "比较 MySQL 切片与 Supabase 向量记录")
    @GetMapping("/session-documents/{documentId}/consistency")
    public AjaxResult consistency(@PathVariable String documentId) {
        accessService.requireSessionDocument(documentId);
        return AjaxResult.success(consistencyService.inspect(documentId));
    }

    @Operation(summary = "修复会话文档向量一致性", description = "删除孤立向量并补写缺失向量")
    @PostMapping("/session-documents/{documentId}/consistency/repair")
    @RateLimiter(key = "ai:session-document:repair:", time = 60, count = 5, limitType = LimitType.USER)
    @RepeatSubmit(interval = 5000, message = "一致性修复正在执行，请勿重复操作")
    public AjaxResult repairConsistency(@PathVariable String documentId) {
        accessService.requireSessionDocument(documentId);
        return AjaxResult.success(consistencyService.repair(documentId));
    }

    @Operation(summary = "删除会话文档", description = "删除会话文档")
    @DeleteMapping("/session-documents/{documentId}")
    public AjaxResult delete(@PathVariable String documentId) {
        // 删除服务统一清理 Supabase 向量、主库切片/元数据和磁盘文件。
        SessionDocument document = accessService.requireSessionDocument(documentId);
        return AjaxResult.success(deletionService.deleteSessionDocument(document));
    }

}
