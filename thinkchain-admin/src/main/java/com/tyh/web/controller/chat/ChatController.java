package com.tyh.web.controller.chat;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.service.ChatService;
import com.tyh.chat.chat.service.ChatStreamService;
import com.tyh.chat.conversation.service.ConversationService;
import com.tyh.chat.model.ModelRegistry;
import com.tyh.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Generic AI chat HTTP API.
 *
 * @Author: GithubTang
 * @Description: AI chat API
 * @Date: 2026/4/29
 * @Version: 1.0
 */
@Tag(name = "AI Chat")
@RestController
@RequestMapping("/ai/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatStreamService chatStreamService;
    private final ConversationService conversationService;
    private final ModelRegistry modelRegistry;

    public ChatController(ChatService chatService,
                          ChatStreamService chatStreamService,
                          ConversationService conversationService,
                          ModelRegistry modelRegistry) {
        this.chatService = chatService;
        this.chatStreamService = chatStreamService;
        this.conversationService = conversationService;
        this.modelRegistry = modelRegistry;
    }

    @Operation(description = "Unified chat")
    @PostMapping("/send")
    public AjaxResult send(@RequestBody ChatRequest request) {
        return chatService.chat(request, null);
    }

    @Operation(description = "Unified streaming chat")
    @PostMapping("/stream")
    public SseEmitter stream(@RequestBody ChatRequest request) {
        return chatStreamService.stream(request, null);
    }

    @Operation(description = "List available models")
    @GetMapping("/models")
    public AjaxResult models() {
        return AjaxResult.success(modelRegistry.listModels());
    }

    @Operation(description = "List conversations")
    @GetMapping("/conversations")
    public AjaxResult conversations(@RequestParam(required = false) String userId) {
        return AjaxResult.success(conversationService.listConversations(userId));
    }

    @Operation(description = "List conversation messages")
    @GetMapping("/conversations/{conversationId}/messages")
    public AjaxResult messages(@PathVariable String conversationId) {
        return AjaxResult.success(conversationService.listMessages(conversationId));
    }

    @Operation(description = "Delete conversation")
    @DeleteMapping("/conversations/{conversationId}")
    public AjaxResult deleteConversation(@PathVariable String conversationId) {
        return AjaxResult.success(conversationService.deleteConversation(conversationId));
    }

    @Operation(description = "Text chat shortcut")
    @PostMapping("/text")
    public AjaxResult text(@RequestParam String model, @RequestParam String message) {
        return chatService.chat(model, message, Set.of("text", "chat"));
    }
}
