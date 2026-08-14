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
 * SSE 对话接口的当前实现。
 *
 * <p>SSE 是服务端通过一个持续的 HTTP 连接向浏览器发送事件的协议。
 * 当前代码先同步获得完整模型结果，再发送 message 和 done 两个事件，
 * 所以它提供了 SSE 接口形态，但还不是厂商模型逐 Token 输出。</p>
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
        // 0L 表示不由 Spring 主动超时；连接在 complete 或 completeWithError 时关闭。
        SseEmitter emitter = new SseEmitter(0L);
        // 使用 Java 21 虚拟线程处理连接，避免长连接占用大量传统平台线程。
        Thread.startVirtualThread(() -> {
            try {
                // 复用普通对话服务，确保能力校验、RAG、会话保存和日志行为保持一致。
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
