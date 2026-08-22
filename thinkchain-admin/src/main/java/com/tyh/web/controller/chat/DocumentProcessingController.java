package com.tyh.web.controller.chat;

import com.tyh.chat.rag.processing.DocumentProcessingService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 文档后台处理的只读运维接口，只返回当前登录用户自己的状态统计。 */
@Tag(name = "AI 文档处理运维")
@RestController
@RequestMapping("/ai/operations/document-processing")
public class DocumentProcessingController {

    private final DocumentProcessingService processingService;
    private final ChatAccessService accessService;

    public DocumentProcessingController(DocumentProcessingService processingService,
                                        ChatAccessService accessService) {
        this.processingService = processingService;
        this.accessService = accessService;
    }

    @Operation(summary = "查询文档处理概况", description = "统计当前用户的待处理、处理中、成功和失败文档")
    @GetMapping
    public AjaxResult summary() {
        return AjaxResult.success(processingService.summary(accessService.currentUserId()));
    }
}
