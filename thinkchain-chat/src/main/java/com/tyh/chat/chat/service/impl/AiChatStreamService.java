package com.tyh.chat.chat.service.impl;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.service.ChatService;
import com.tyh.chat.chat.service.ChatStreamService;
import com.tyh.common.core.domain.AjaxResult;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;

/**
 * SSE 对话接口的当前实现。
 *
 * <p>SSE 是服务端通过一个持续的 HTTP 连接向浏览器发送事件的协议。
 * DashScope 每产生一段新文字就发送 delta，结束后发送 result 和 done；异常发送 error。</p>
 *
 * @Author: GithubTang
 * @Description: 流式对话服务实现
 * @Date: 2026/4/29
 * @Version: 1.0
 */
@Service
public class AiChatStreamService implements ChatStreamService {

    private static final long STREAM_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    private final ChatService chatService;

    public AiChatStreamService(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public SseEmitter stream(ChatRequest request, Set<String> requiredCapabilities) {
        // 设置上限避免断开的客户端长期占用服务端资源。
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        // 使用 Java 21 虚拟线程处理连接，避免长连接占用大量传统平台线程。
        Thread.startVirtualThread(() -> {
            try {
                // 流式服务与普通对话复用同一业务编排；每个 delta 都是模型新生成的文本。
                AjaxResult result = chatService.chatStreaming(request, requiredCapabilities, delta -> {
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(delta));
                    } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                    }
                });
                if (result.isError()) {
                    emitter.send(SseEmitter.event().name("error")
                            .data(String.valueOf(result.get(AjaxResult.MSG_TAG))));
                    emitter.complete();
                    return;
                }
                emitter.send(SseEmitter.event().name("result").data(result));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("流式对话失败"));
                    emitter.complete();
                } catch (IOException sendException) {
                    emitter.completeWithError(e);
                }
            }
        });
        return emitter;
    }
}
