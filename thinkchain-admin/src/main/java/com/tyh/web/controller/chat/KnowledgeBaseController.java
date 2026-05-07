package com.tyh.web.controller.chat;

import com.tyh.chat.rag.knowledge.domain.KnowledgeBase;
import com.tyh.chat.rag.knowledge.service.KnowledgeBaseService;
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

@Tag(name = "AI Knowledge Base")
@RestController
@RequestMapping("/ai/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Operation(description = "Create knowledge base")
    @PostMapping
    public AjaxResult create(@RequestBody KnowledgeBase knowledgeBase) {
        return AjaxResult.success(knowledgeBaseService.create(knowledgeBase));
    }

    @Operation(description = "List knowledge bases")
    @GetMapping
    public AjaxResult list(KnowledgeBase query) {
        return AjaxResult.success(knowledgeBaseService.list(query));
    }

    @Operation(description = "Get knowledge base")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable String id) {
        return AjaxResult.success(knowledgeBaseService.getById(id));
    }

    @Operation(description = "Update knowledge base")
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable String id, @RequestBody KnowledgeBase knowledgeBase) {
        knowledgeBase.setId(id);
        return AjaxResult.success(knowledgeBaseService.update(knowledgeBase));
    }

    @Operation(description = "Delete knowledge base")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable String id) {
        return AjaxResult.success(knowledgeBaseService.deleteById(id));
    }
}
