package com.tyh.web.controller.chat;

import com.tyh.chat.rag.knowledge.domain.KnowledgeBase;
import com.tyh.chat.rag.knowledge.dto.KnowledgeBaseCreateRequest;
import com.tyh.chat.rag.knowledge.dto.KnowledgeBaseUpdateRequest;
import com.tyh.chat.rag.knowledge.service.KnowledgeBaseService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.security.ChatResourceDeletionService;
import com.tyh.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 长期知识库的 HTTP 管理接口。
 *
 * <p>知识库是文档的逻辑分组。本类管理知识库元数据；上传、解析和向量化文档由 KnowledgeDocumentController 负责。
 * 所有查询条件中的 userId 都由服务端根据 JWT 设置，客户端无法查询其他用户的数据。</p>
 */
@Tag(name = "AI 知识库")
@RestController
@RequestMapping("/ai/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatAccessService accessService;
    private final ChatResourceDeletionService deletionService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                   ChatAccessService accessService,
                                   ChatResourceDeletionService deletionService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.accessService = accessService;
        this.deletionService = deletionService;
    }

    @Operation(summary = "创建知识库", description = "创建知识库")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        // 请求 DTO 没有 userId 和统计字段，资源归属只能由服务端写入。
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(accessService.currentUserId());
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setStatus(request.getStatus());
        knowledgeBaseService.create(knowledgeBase);
        return AjaxResult.success(knowledgeBase);
    }

    @Operation(summary = "查询知识库列表", description = "查询知识库列表")
    @GetMapping
    public AjaxResult list(KnowledgeBase query) {
        query.setUserId(accessService.currentUserId());
        return AjaxResult.success(knowledgeBaseService.list(query));
    }

    @Operation(summary = "查询知识库详情", description = "查询知识库详情")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable String id) {
        return AjaxResult.success(accessService.requireKnowledgeBase(id));
    }

    @Operation(summary = "更新知识库", description = "更新知识库")
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable String id, @Valid @RequestBody KnowledgeBaseUpdateRequest request) {
        accessService.requireKnowledgeBase(id);
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(id);
        knowledgeBase.setUserId(accessService.currentUserId());
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setStatus(request.getStatus());
        return AjaxResult.success(knowledgeBaseService.update(knowledgeBase) > 0);
    }

    @Operation(summary = "删除知识库", description = "删除知识库")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable String id) {
        // 级联服务会先清理库内文档及其向量/切片/文件，再删除知识库记录。
        accessService.requireKnowledgeBase(id);
        return AjaxResult.success(deletionService.deleteKnowledgeBase(
                id, accessService.currentUserId()));
    }
}
