package com.tyh.chat.service.langchain;

import com.tyh.common.core.domain.AjaxResult;
import dev.langchain4j.model.chat.ChatModel;

/**
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
        String response = chatModel.chat(userMessage);
        return AjaxResult.success(response);
    }
}
