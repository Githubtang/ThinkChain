package com.tyh.chat.chat.service;

import com.tyh.chat.chat.dto.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Set;

/**
 * 流式对话服务，第一阶段提供统一入口。
 *
 * @Author: GithubTang
 * @Description: 流式对话服务
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public interface ChatStreamService {

    SseEmitter stream(ChatRequest request, Set<String> requiredCapabilities);
}
