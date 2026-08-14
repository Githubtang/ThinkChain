package com.tyh.web.controller.chat;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.chat.service.ChatService;
import com.tyh.chat.security.ChatAccessService;
import com.tyh.chat.validation.ChatRequestValidator;
import com.tyh.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 早期千问兼容接口。
 *
 * <p>该类已标记 {@link Deprecated}，内部已经委托统一 ChatService，并继续执行当前用户和参数校验。
 * 新代码应优先使用 /ai/chat/send；保留本接口是为了避免已有调用方立即失效。</p>
 *
 * @Author: GithubTang
 * @Description: AI 对话 HTTP 接口（基础文本、图文多模态），委托 {@link ChatService}
 * @Date: 2026/3/23 17:14
 * @Version: 1.0
 */
@Tag(name = "Ai对话")
@RestController
@Deprecated
@RequestMapping("/ai/qwen")
public class QwenPLusController {
    
    private final ChatService chatService;
    private final ChatAccessService accessService;
    private final ChatRequestValidator requestValidator;
    
    public QwenPLusController(ChatService chatService,
                              ChatAccessService accessService,
                              ChatRequestValidator requestValidator) {
        this.chatService = chatService;
        this.accessService = accessService;
        this.requestValidator = requestValidator;
    }
    
    @Operation(summary = "基础对话", description = "基础对话")
    @PostMapping("/send")
    public AjaxResult sendMessage(@RequestBody Map<String, String> requestBody) {
        String model = requestBody.getOrDefault("model", "qwen3.5-plus");
        String message = requestBody.get("message");
        ChatRequest request = textRequest(model, message);
        requestValidator.validate(request);
        accessService.prepare(request);
        return chatService.chat(request, Set.of("text", "chat"));
    }

    @Operation(summary = "图片对话", description = "图片对话")
    @PostMapping("/image")
    public AjaxResult imageMessage(@RequestBody Map<String, String> requestBody) {
        String model = requestBody.getOrDefault("model", "qwen3.5-plus");
        String message = requestBody.get("message");
        String image = requestBody.get("image");

        ChatRequest req = new ChatRequest();
        req.setModel(model);
        Message user = new Message();
        user.setRole("user");
        List<Content> contents = new ArrayList<>();
        if (message != null && !message.isBlank()) {
            Content text = new Content();
            text.setType("text");
            text.setText(message);
            contents.add(text);
        }
        if (image != null && !image.isBlank()) {
            Content img = new Content();
            img.setType("image");
            if (image.startsWith("http://") || image.startsWith("https://")) {
                img.setUrl(image);
            } else {
                img.setText(image);
            }
            contents.add(img);
        }
        user.setContents(contents);
        req.setMessages(List.of(user));
        requestValidator.validate(req);
        accessService.prepare(req);
        return chatService.chat(req, Set.of("text", "chat", "image"));
    }

    private static ChatRequest textRequest(String model, String message) {
        ChatRequest request = new ChatRequest();
        request.setModel(model);
        Message user = new Message();
        user.setRole("user");
        Content content = new Content();
        content.setType("text");
        content.setText(message);
        user.setContents(List.of(content));
        request.setMessages(List.of(user));
        return request;
    }
}
