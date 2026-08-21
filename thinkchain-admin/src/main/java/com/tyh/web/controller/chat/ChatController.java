package com.tyh.web.controller.chat;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.chat.service.ChatService;
import com.tyh.chat.chat.service.ChatStreamService;
import com.tyh.chat.conversation.service.ConversationService;
import com.tyh.chat.model.ModelRegistry;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.security.ChatResourceDeletionService;
import com.tyh.chat.validation.ChatRequestValidator;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.core.controller.BaseController;
import com.tyh.common.core.page.TableDataInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;
import java.util.List;

/**
 * 通用 AI 对话 HTTP 接口，是前端使用聊天功能的主要入口。
 *
 * <p>Controller 只负责接收 HTTP 参数、触发参数/权限校验并调用业务 Service，
 * 不在这里直接调用模型 SDK，也不直接操作数据库。</p>
 *
 * <p>调用顺序通常是：请求校验 → 绑定当前登录用户并检查资源归属 → ChatService 业务编排 → AjaxResult 返回。</p>
 *
 * @Author: GithubTang
 * @Description: AI 对话接口
 * @Date: 2026/4/29
 * @Version: 1.0
 */
@Tag(name = "AI 对话")
@Validated
@RestController
@RequestMapping("/ai/chat")
public class ChatController extends BaseController {

    private final ChatService chatService;
    private final ChatStreamService chatStreamService;
    private final ConversationService conversationService;
    private final ModelRegistry modelRegistry;
    private final ChatAccessService accessService;
    private final ChatResourceDeletionService deletionService;
    private final ChatRequestValidator requestValidator;

    public ChatController(ChatService chatService,
                          ChatStreamService chatStreamService,
                          ConversationService conversationService,
                          ModelRegistry modelRegistry,
                          ChatAccessService accessService,
                          ChatResourceDeletionService deletionService,
                          ChatRequestValidator requestValidator) {
        this.chatService = chatService;
        this.chatStreamService = chatStreamService;
        this.conversationService = conversationService;
        this.modelRegistry = modelRegistry;
        this.accessService = accessService;
        this.deletionService = deletionService;
        this.requestValidator = requestValidator;
    }

    @Operation(summary = "统一对话", description = "统一对话")
    @PostMapping("/send")
    public AjaxResult send(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ChatRequest.class),
                            examples = @ExampleObject(value = "{\"model\":\"qwen-plus\","
                                    + "\"messages\":[{\"role\":\"user\","
                                    + "\"contents\":[{\"type\":\"text\",\"text\":\"你好\"}]}],"
                                    + "\"options\":{\"temperature\":0.7,\"maxTokens\":2048}}")))
            @Valid @RequestBody ChatRequest request) {
        // @Valid 检查字段长度等基础规则，requestValidator 再检查至少一条 user 消息等跨字段规则。
        requestValidator.validate(request);
        // prepare 使用 JWT 用户覆盖客户端 userId，并检查会话、知识库和会话文档归属。
        accessService.prepare(request);
        return chatService.chat(request, null);
    }

    @Operation(summary = "统一流式对话", description = "统一流式对话")
    @PostMapping("/stream")
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        requestValidator.validate(request);
        accessService.prepare(request);
        return chatStreamService.stream(request, null);
    }

    @Operation(summary = "查询可用模型", description = "查询可用模型")
    @GetMapping("/models")
    public AjaxResult models() {
        // 只返回公开模型摘要，不返回配置文件中的 API Key 和 Base URL。
        return AjaxResult.success(modelRegistry.listPublicModels());
    }

    @Operation(summary = "查询会话列表", description = "查询会话列表")
    @GetMapping("/conversations")
    public TableDataInfo conversations() {
        startPage();
        return getDataTable(conversationService.listConversations(accessService.currentUserId()));
    }

    @Operation(summary = "查询会话消息列表", description = "查询会话消息列表")
    @GetMapping("/conversations/{conversationId}/messages")
    public TableDataInfo messages(@PathVariable String conversationId) {
        accessService.requireConversation(conversationId);
        startPage();
        return getDataTable(conversationService.listMessages(conversationId));
    }

    @Operation(summary = "删除会话", description = "删除会话")
    @DeleteMapping("/conversations/{conversationId}")
    public AjaxResult deleteConversation(@PathVariable String conversationId) {
        // 先检查所有权，再通过级联删除服务清理会话文档、切片、向量、消息和会话。
        accessService.requireConversation(conversationId);
        return AjaxResult.success(deletionService.deleteConversation(
                conversationId, accessService.currentUserId()));
    }

    @Operation(summary = "文本对话快捷入口", description = "文本对话快捷入口")
    @PostMapping("/text")
    public AjaxResult text(
            @NotBlank(message = "模型名称不能为空") @Size(max = 100) @RequestParam String model,
            @NotBlank(message = "消息不能为空") @Size(max = 20000) @RequestParam String message) {
        ChatRequest request = new ChatRequest();
        request.setModel(model);
        Message user = new Message();
        user.setRole("user");
        Content content = new Content();
        content.setType("text");
        content.setText(message);
        user.setContents(List.of(content));
        request.setMessages(List.of(user));
        requestValidator.validate(request);
        accessService.prepare(request);
        return chatService.chat(request, Set.of("text", "chat"));
    }
}
