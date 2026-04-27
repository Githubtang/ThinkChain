package com.tyh.web.controller.chat;

import com.tyh.chat.dto.ChatRequest;
import com.tyh.chat.dto.Content;
import com.tyh.chat.dto.Message;
import com.tyh.chat.service.ChatService;
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
 * @Author: GithubTang
 * @Description: AI 对话 HTTP 接口（基础文本、图文多模态），委托 {@link ChatService}
 * @Date: 2026/3/23 17:14
 * @Version: 1.0
 */
@Tag(name = "Ai对话")
@RestController
@RequestMapping("/ai/chat")
public class QwenPLusController {
    
    private final ChatService chatService;
    
    public QwenPLusController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    @Operation(description = "基础对话")
    @PostMapping("/send")
    public AjaxResult sendMessage(@RequestBody Map<String, String> requestBody) {
        String model = requestBody.getOrDefault("model", "qwen3.5-plus");
        String message = requestBody.get("message");
        return chatService.chat(model, message, Set.of("text", "chat"));
    }

    @Operation(description = "图片对话")
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
        return chatService.chat(req, Set.of("text", "chat", "image"));
    }
}
