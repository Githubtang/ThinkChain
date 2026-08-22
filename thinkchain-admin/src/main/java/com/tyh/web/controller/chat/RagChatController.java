package com.tyh.web.controller.chat;

import com.tyh.chat.rag.chat.dto.RagChatRequest;
import com.tyh.chat.rag.chat.service.RagChatService;
import com.tyh.chat.rag.log.domain.RagQueryLog;
import com.tyh.chat.rag.log.service.RagQueryLogService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.validation.ChatRequestValidator;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.annotation.RateLimiter;
import com.tyh.common.enums.LimitType;
import com.tyh.common.core.controller.BaseController;
import com.tyh.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 独立 RAG 问答和检索日志接口。
 *
 * <p>与普通聊天接口不同，/chat 明确要求按照 ragMode 选择资料范围，并在响应中返回引用来源。
 * 日志查询会强制绑定当前用户，不能通过请求参数查看其他用户的检索记录。</p>
 */
@Tag(name = "AI RAG 对话")
@RestController
@RequestMapping("/ai/rag")
public class RagChatController extends BaseController {

    private final RagChatService ragChatService;
    private final RagQueryLogService queryLogService;
    private final ChatAccessService accessService;
    private final ChatRequestValidator requestValidator;

    public RagChatController(RagChatService ragChatService,
                             RagQueryLogService queryLogService,
                             ChatAccessService accessService,
                             ChatRequestValidator requestValidator) {
        this.ragChatService = ragChatService;
        this.queryLogService = queryLogService;
        this.accessService = accessService;
        this.requestValidator = requestValidator;
    }

    @Operation(summary = "RAG 对话", description = "RAG 对话")
    @PostMapping("/chat")
    @RateLimiter(key = "ai:rag:chat:", time = 60, count = 20, limitType = LimitType.USER)
    public AjaxResult chat(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RagChatRequest.class),
                            examples = @ExampleObject(value = "{\"conversationId\":\"会话ID\","
                                    + "\"model\":\"qwen-plus\",\"question\":\"根据资料回答问题\","
                                    + "\"knowledgeBaseIds\":[\"知识库ID\"],"
                                    + "\"ragMode\":\"KB_ONLY\",\"topK\":6}")))
            @Valid @RequestBody RagChatRequest request) {
        // 先验证模式与资料范围的组合，再验证所有引用资源都归当前用户所有。
        requestValidator.validate(request);
        accessService.prepare(request);
        return AjaxResult.success(ragChatService.chat(request));
    }

    @Operation(summary = "查询 RAG 检索日志", description = "查询 RAG 检索日志")
    @GetMapping("/query-logs")
    public TableDataInfo logs(RagQueryLog query) {
        query.setUserId(accessService.currentUserId());
        startPage();
        return getDataTable(queryLogService.list(query));
    }
}
