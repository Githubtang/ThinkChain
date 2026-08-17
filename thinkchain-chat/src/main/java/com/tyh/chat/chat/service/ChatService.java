package com.tyh.chat.chat.service;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.common.core.domain.AjaxResult;

import java.util.Set;
import java.util.function.Consumer;

/**
 * AI 对话应用服务。
 *
 * @Author: GithubTang
 * @Description: 对话服务门面
 * @Date: 2026/4/29
 * @Version: 1.0
 */
/**
 * AI 对话的统一业务入口。
 *
 * <p>控制器只需要组装 {@link ChatRequest}，不需要了解通义千问等厂商 SDK 的调用方式。
 * 实现类会继续完成模型查找、能力校验、RAG 上下文补充、会话保存、模型调用和日志记录。</p>
 */
public interface ChatService {

    /**
     * 使用完整请求调用指定模型。
     *
     * @param request 对话请求，包含模型逻辑名称、消息、调用参数和可选的 RAG 范围
     * @param requiredCapabilities 接口额外要求的模型能力，例如 text、chat、image；没有额外要求时传 null
     * @return 统一接口结果；成功时 data 是 ChatResponse，失败时包含可安全返回给前端的错误信息
     */
    AjaxResult chat(ChatRequest request, Set<String> requiredCapabilities);

    /**
     * 纯文本对话的便捷方法。内部会把字符串转换为标准 ChatRequest 后复用完整调用流程。
     *
     * @param modelName application-ai.yml 中配置的模型逻辑名称，不是厂商类名
     * @param userInput 用户输入的文本
     * @param requiredCapabilities 接口要求的能力集合
     * @return 统一接口结果
     */
    AjaxResult chat(String modelName, String userInput, Set<String> requiredCapabilities);

    /**
     * 执行完整聊天业务链，同时把厂商流式返回的新文本片段交给 onDelta。
     * 会话、消息和日志仍在流式结束后统一保存。
     */
    AjaxResult chatStreaming(ChatRequest request, Set<String> requiredCapabilities, Consumer<String> onDelta);
}
