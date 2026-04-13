package com.tyh.chat.service.langchain;

import com.tyh.common.core.domain.AjaxResult;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.data.message.SystemMessage.systemMessage;
import static dev.langchain4j.data.message.UserMessage.userMessage;

/**
 * 基于注入的 LangChain4j {@link ChatModel} 的示例实现（非当前主流程；主流程见 {@link com.tyh.chat.service.impl.AiChatService}）。
 *
 * @Author: GithubTang
 * @Description: LangChain4j ChatModel 直连对话示例
 * @Date: 2026/3/29
 * @Version: 1.0
 */
public class ChatServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatModel chatModel;
    private static final String DEFAULT_SYSTEM_PROMPT = "你是一个乐于助人的 AI 助手。";

    public ChatServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
        log.info("ChatServiceImpl initialized with ChatModel: {}", chatModel.getClass().getSimpleName());
    }

    public AjaxResult chat(String message) {
        log.info("Received chat message: {}", message);
        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(systemMessage(DEFAULT_SYSTEM_PROMPT));
        chatMessages.add(userMessage(message));
        ChatResponse response = chatModel.chat(chatMessages);
        chatModel.doChat(ChatRequest.builder()
                .messages(chatMessages)
                .build());
        log.info("ChatModel responded with: {}", response.aiMessage().text());
        return AjaxResult.success(response.aiMessage().text());
    }
}
