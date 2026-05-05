package com.tyh.chat.conversation.service.impl;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.conversation.domain.ChatConversation;
import com.tyh.chat.conversation.domain.ChatMessage;
import com.tyh.chat.conversation.mapper.ChatConversationMapper;
import com.tyh.chat.conversation.mapper.ChatMessageMapper;
import com.tyh.chat.conversation.service.ConversationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 会话持久化服务实现。
 *
 * @Author: GithubTang
 * @Description: 会话服务实现
 * @Date: 2026/4/29
 * @Version: 1.0
 */
@Service
public class ConversationServiceImpl implements ConversationService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;

    public ConversationServiceImpl(ChatConversationMapper conversationMapper, ChatMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    public ChatConversation ensureConversation(ChatRequest request) {
        String conversationId = request.getConversationId();
        if (conversationId != null && !conversationId.isBlank()) {
            ChatConversation existing = conversationMapper.selectChatConversationById(conversationId);
            if (existing != null) {
                return existing;
            }
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setUserId(request.getUserId());
        conversation.setModel(request.getModel());
        conversation.setSystemPrompt(request.getSystemPrompt());
        conversation.setTitle(buildTitle(request));
        conversationMapper.insertChatConversation(conversation);
        request.setConversationId(conversation.getId());
        return conversation;
    }

    @Override
    public ChatMessage saveUserMessage(String conversationId, String model, Message message) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setConversationId(conversationId);
        chatMessage.setRole(message != null ? message.getRole() : "user");
        chatMessage.setContentType(firstContentType(message));
        chatMessage.setContent(flattenText(message));
        chatMessage.setRawContent(flattenRaw(message));
        chatMessage.setModel(model);
        messageMapper.insertChatMessage(chatMessage);
        return chatMessage;
    }

    @Override
    public ChatMessage saveAssistantMessage(String conversationId, String model, String content, String rawContent) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setConversationId(conversationId);
        chatMessage.setRole("assistant");
        chatMessage.setContentType("text");
        chatMessage.setContent(content);
        chatMessage.setRawContent(rawContent);
        chatMessage.setModel(model);
        messageMapper.insertChatMessage(chatMessage);
        return chatMessage;
    }

    @Override
    public List<ChatConversation> listConversations(String userId) {
        ChatConversation query = new ChatConversation();
        query.setUserId(userId);
        return conversationMapper.selectChatConversationList(query);
    }

    @Override
    public List<ChatMessage> listMessages(String conversationId) {
        ChatMessage query = new ChatMessage();
        query.setConversationId(conversationId);
        return messageMapper.selectChatMessageList(query);
    }

    @Override
    public int deleteConversation(String conversationId) {
        messageMapper.deleteChatMessagesByConversationId(conversationId);
        return conversationMapper.deleteChatConversationById(conversationId);
    }

    private static String buildTitle(ChatRequest request) {
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            return "New Chat";
        }
        String text = flattenText(request.getMessages().get(0));
        if (text == null || text.isBlank()) {
            return "New Chat";
        }
        return text.length() > 30 ? text.substring(0, 30) : text;
    }

    private static String firstContentType(Message message) {
        if (message == null || message.getContents() == null || message.getContents().isEmpty()) {
            return "text";
        }
        Content first = message.getContents().get(0);
        return first != null && first.getType() != null ? first.getType() : "text";
    }

    private static String flattenText(Message message) {
        if (message == null || message.getContents() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Content content : message.getContents()) {
            if (content != null && "text".equalsIgnoreCase(content.getType()) && content.getText() != null) {
                sb.append(content.getText());
            }
        }
        return sb.toString();
    }

    private static String flattenRaw(Message message) {
        if (message == null || message.getContents() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Content content : message.getContents()) {
            if (content == null) {
                continue;
            }
            sb.append('[').append(content.getType()).append(']');
            if (content.getText() != null) {
                sb.append(content.getText());
            }
            if (content.getUrl() != null) {
                sb.append(content.getUrl());
            }
        }
        return sb.toString();
    }
}
