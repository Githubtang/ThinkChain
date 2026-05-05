package com.tyh.chat.chat.service.impl;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.service.ChatService;
import com.tyh.chat.chat.service.ChatStreamService;
import com.tyh.common.core.domain.AjaxResult;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;

/**
 * 第一阶段流式入口，将同步调用封装为 SSE 响应。
 *
 * @Author: GithubTang
 * @Description: 流式对话服务实现
 * @Date: 2026/4/29
 * @Version: 1.0
 */
@Service
public class AiChatStreamService implements ChatStreamService {

    private final ChatService chatService;

    public AiChatStreamService(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public SseEmitter stream(ChatRequest request, Set<String> requiredCapabilities) {
        SseEmitter emitter = new SseEmitter(0L);
        Thread.startVirtualThread(() -> {
            try {
                AjaxResult result = chatService.chat(request, requiredCapabilities);
                emitter.send(SseEmitter.event().name("message").data(result));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
