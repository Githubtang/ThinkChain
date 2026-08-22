package com.tyh.chat.chat.service.impl;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.service.ChatService;
import com.tyh.chat.chat.service.ChatStreamService;
import com.tyh.chat.security.ChatStreamConcurrencyGuard;
import com.tyh.common.core.domain.AjaxResult;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    private final ChatStreamConcurrencyGuard concurrencyGuard;

    public AiChatStreamService(ChatService chatService, ChatStreamConcurrencyGuard concurrencyGuard) {
        this.chatService = chatService;
        this.concurrencyGuard = concurrencyGuard;
    }

    @Override
    public SseEmitter stream(ChatRequest request, Set<String> requiredCapabilities) {
        // accessService 已将 JWT 用户写入 request；先占用名额，再创建长连接。
        ChatStreamConcurrencyGuard.Permit permit = concurrencyGuard.acquire(request.getUserId());
        // 设置上限避免断开的客户端长期占用服务端资源。
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<Thread> workerReference = new AtomicReference<>();
        Runnable cancelWorker = () -> {
            closed.set(true);
            permit.close();
            Thread worker = workerReference.get();
            if (worker != null && worker != Thread.currentThread()) {
                worker.interrupt();
            }
        };
        // 无论是浏览器主动断开、服务端超时还是连接异常，都中断负责厂商流式调用的虚拟线程。
        emitter.onTimeout(cancelWorker);
        emitter.onCompletion(cancelWorker);
        emitter.onError(error -> cancelWorker.run());

        // 使用 Java 21 虚拟线程处理连接，避免长连接占用大量传统平台线程。
        Thread worker = Thread.ofVirtual().name("ai-chat-stream").unstarted(() -> {
            try {
                // 流式服务与普通对话复用同一业务编排；每个 delta 都是模型新生成的文本。
                AjaxResult result = chatService.chatStreaming(request, requiredCapabilities, delta -> {
                    if (closed.get() || Thread.currentThread().isInterrupted()) {
                        throw new CancellationException("SSE connection closed");
                    }
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(delta));
                    } catch (IOException exception) {
                        closed.set(true);
                        throw new UncheckedIOException(exception);
                    }
                });
                if (closed.get() || Thread.currentThread().isInterrupted()) {
                    return;
                }
                if (result.isError()) {
                    emitter.send(SseEmitter.event().name("error")
                            .data(String.valueOf(result.get(AjaxResult.MSG_TAG))));
                    closed.set(true);
                    emitter.complete();
                    return;
                }
                emitter.send(SseEmitter.event().name("result").data(result));
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                closed.set(true);
                emitter.complete();
            } catch (CancellationException | UncheckedIOException exception) {
                // 客户端断开是正常连接生命周期，不再尝试向已经关闭的连接发送 error。
                closed.set(true);
            } catch (Exception e) {
                if (closed.get() || Thread.currentThread().isInterrupted()) {
                    return;
                }
                try {
                    emitter.send(SseEmitter.event().name("error").data("流式对话失败"));
                    closed.set(true);
                    emitter.complete();
                } catch (IOException sendException) {
                    closed.set(true);
                    emitter.completeWithError(e);
                }
            } finally {
                permit.close();
            }
        });
        workerReference.set(worker);
        try {
            worker.start();
        } catch (RuntimeException exception) {
            permit.close();
            throw exception;
        }
        return emitter;
    }
}
