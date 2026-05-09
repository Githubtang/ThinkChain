package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chat.dto.RagChatRequest;
import com.tyh.chat.rag.chat.service.RagChatService;
import com.tyh.chat.rag.log.domain.RagQueryLog;
import com.tyh.chat.rag.log.service.RagQueryLogService;
import com.tyh.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI RAG 对话")
@RestController
@RequestMapping("/ai/rag")
public class RagChatController {

    private final RagChatService ragChatService;
    private final RagQueryLogService queryLogService;

    public RagChatController(RagChatService ragChatService, RagQueryLogService queryLogService) {
        this.ragChatService = ragChatService;
        this.queryLogService = queryLogService;
    }

    @Operation(summary = "RAG 对话", description = "RAG 对话")
    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody RagChatRequest request) {
        try {
            return AjaxResult.success(ragChatService.chat(request));
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    @Operation(summary = "查询 RAG 检索日志", description = "查询 RAG 检索日志")
    @GetMapping("/query-logs")
    public AjaxResult logs(RagQueryLog query) {
        return AjaxResult.success(queryLogService.list(query));
    }
}
