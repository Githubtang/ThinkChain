package com.tyh.chat.conversation.service;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.conversation.domain.ChatConversation;
import com.tyh.chat.conversation.domain.ChatMessage;

import java.util.List;

/**
 * 会话持久化服务。
 *
 * @Author: GithubTang
 * @Description: 会话服务
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public interface ConversationService {

    ChatConversation ensureConversation(ChatRequest request);

    ChatMessage saveUserMessage(String conversationId, String model, Message message);

    ChatMessage saveAssistantMessage(String conversationId, String model, String content, String rawContent);

    List<ChatConversation> listConversations(String userId);

    List<ChatMessage> listMessages(String conversationId);

    int deleteConversation(String conversationId);
}
