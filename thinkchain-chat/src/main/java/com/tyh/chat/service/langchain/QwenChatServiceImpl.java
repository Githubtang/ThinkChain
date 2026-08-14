package com.tyh.chat.service.langchain;

import com.tyh.common.core.domain.AjaxResult;
import dev.langchain4j.model.chat.ChatModel;

/**
 * 最早期的 LangChain4j 千问调用示例。
 *
 * <p><strong>本类不是 Spring Bean，也不在当前 HTTP 调用链中。</strong>
 * 正式千问调用由 DashScopeSdkChatAdapter 完成，本类仅展示 ChatModel.chat(String) 的最简用法。</p>
 *
 * @Author: GithubTang
 * @Description: 基于 LangChain4j ChatModel 的千问对话示例（非厂商 SDK 主路径）
 * @Date: 2026/3/24 16:26
 * @Version: 1.0
 */
public class QwenChatServiceImpl {

    ChatModel chatModel;

    public QwenChatServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public AjaxResult chat(String userMessage) {
        // 该写法没有会话、权限、RAG、日志和统一模型选择能力，只适合教学示例。
        String response = chatModel.chat(userMessage);
        return AjaxResult.success(response);
    }
}
