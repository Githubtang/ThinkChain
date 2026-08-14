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
 * 基于注入的 LangChain4j {@link ChatModel} 的早期直连示例。
 *
 * <p><strong>它不是当前正式对话流程，也没有注册为 Spring Service。</strong>
 * 当前 HTTP 接口使用 {@link com.tyh.chat.chat.service.impl.AiChatService}，后者支持模型注册、
 * 厂商适配、能力校验、会话、RAG 和日志。保留本类只是便于理解 LangChain4j 最基础的调用方式。</p>
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
        // 示例手工构造 system/user 两条消息；正式业务请调用 com.tyh.chat.chat.service.ChatService。
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
