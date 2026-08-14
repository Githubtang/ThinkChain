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
/**
 * 对话的 SSE 流式输出接口。
 *
 * <p>SSE（Server-Sent Events）允许服务端通过一个持续连接分批向浏览器发送事件。
 * 当前实现仍先获得完整模型结果，再按事件格式发送；它提供的是接口形态，不代表厂商 SDK 已实现逐 Token 流式调用。</p>
 */
public interface ChatStreamService {

    /**
     * 创建一次 SSE 对话连接。
     *
     * @param request 标准对话请求
     * @param requiredCapabilities 当前接口要求的模型能力
     * @return Spring 的 SSE 连接对象，控制器直接返回给客户端
     */
    SseEmitter stream(ChatRequest request, Set<String> requiredCapabilities);
}
