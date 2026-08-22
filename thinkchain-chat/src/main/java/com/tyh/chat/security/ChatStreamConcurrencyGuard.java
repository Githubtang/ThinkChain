package com.tyh.chat.security;

import com.tyh.common.constant.HttpStatus;
import com.tyh.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单实例下的用户级 SSE 并发保护器。
 *
 * <p>流式对话会长期占用模型连接。调用 {@link #acquire(String)} 成功后必须关闭返回的 Permit；
 * Permit 自身是幂等的，因此超时、浏览器断开和正常完成可以安全地重复调用 close。</p>
 */
@Component
public class ChatStreamConcurrencyGuard {

    private final ChatProtectionProperties properties;
    private final ConcurrentHashMap<String, AtomicInteger> activeStreams = new ConcurrentHashMap<>();

    public ChatStreamConcurrencyGuard(ChatProtectionProperties properties) {
        this.properties = properties;
    }

    /** 获取一个流式连接名额；超过用户并发上限时抛出 429 业务异常。 */
    public Permit acquire(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ServiceException("无法识别当前用户", HttpStatus.UNAUTHORIZED);
        }
        int maximum = Math.max(1, properties.getMaxConcurrentStreams());
        AtomicInteger counter = activeStreams.computeIfAbsent(userId, key -> new AtomicInteger());
        int current = counter.incrementAndGet();
        if (current > maximum) {
            release(userId, counter);
            throw new ServiceException("当前流式对话连接过多，请稍候再试", HttpStatus.TOO_MANY_REQUESTS);
        }
        return new Permit(() -> release(userId, counter));
    }

    /** 返回某个用户当前占用的连接数，主要供测试和运维检查使用。 */
    public int activeCount(String userId) {
        AtomicInteger counter = activeStreams.get(userId);
        return counter != null ? Math.max(0, counter.get()) : 0;
    }

    private void release(String userId, AtomicInteger counter) {
        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            activeStreams.remove(userId, counter);
        }
    }

    /** 表示已经取得的一个 SSE 名额，close 后会归还。 */
    public static final class Permit implements AutoCloseable {
        private final Runnable release;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private Permit(Runnable release) {
            this.release = release;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                release.run();
            }
        }
    }
}
