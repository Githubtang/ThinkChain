package com.tyh.chat.capability;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * 根据 {@link ChatRequest} 中各 {@link Content#type} 推导本次调用所需的 capability 标签，
 * 与 {@code application-ai.yml} 中模型的 {@code capabilities} 做交集校验；默认包含 {@code chat}。
 *
 * @Author: GithubTang
 * @Description: 从请求内容推导模型能力标签，供 CapabilityValidator 使用
 * @Date: 2026/4/11
 * @Version: 1.0
 */
public final class ChatCapabilityDeriver {

    private ChatCapabilityDeriver() {
    }

    /**
     * 推导本次请求隐含的能力集合。
     *
     * @param request 统一对话请求，可为 null
     * @return 能力标签集合（至少含 chat）
     */
    public static Set<String> derive(ChatRequest request) {
        Set<String> caps = new LinkedHashSet<>();
        caps.add("chat");
        if (request == null || request.getMessages() == null) {
            return caps;
        }
        for (Message m : request.getMessages()) {
            if (m == null || m.getContents() == null) {
                continue;
            }
            for (Content c : m.getContents()) {
                if (c == null || c.getType() == null || c.getType().isBlank()) {
                    continue;
                }
                String t = c.getType().trim().toLowerCase(Locale.ROOT);
                switch (t) {
                    case "text" -> caps.add("text");
                    case "image" -> caps.add("image");
                    case "file", "document" -> caps.add("document");
                    case "video" -> caps.add("video");
                    case "audio" -> caps.add("audio");
                    default -> {
                        // 未知类型不自动加入，避免误放行
                    }
                }
            }
        }
        return caps;
    }
}
