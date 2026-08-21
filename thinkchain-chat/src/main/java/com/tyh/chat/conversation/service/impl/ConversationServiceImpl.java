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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 会话持久化实现，通过 MyBatis Mapper 操作会话表和消息表。
 *
 * <p>新会话会从第一条用户文本生成简短标题；消息同时保存扁平文本和原始 JSON：
 * 扁平文本方便展示，原始 JSON 用来保留图片、文件等多模态结构。</p>
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
    public ChatConversation getConversation(String conversationId) {
        return conversationMapper.selectChatConversationById(conversationId);
    }

    @Override
    public ChatConversation ensureConversation(ChatRequest request) {
        // 带会话 ID 表示继续已有对话。资源归属已经由控制器入口的 ChatAccessService 校验。
        String conversationId = request.getConversationId();
        if (conversationId != null && !conversationId.isBlank()) {
            ChatConversation existing = conversationMapper.selectChatConversationById(conversationId);
            if (existing != null) {
                return existing;
            }
        }

        // 不带 ID 或原会话不存在时创建新会话，并回写 ID 供后续消息关联。
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
    @Transactional
    public ChatMessage saveUserMessage(String conversationId, String model, Message message) {
        // content 保存便于阅读的文本；rawContent 保存完整 Message JSON，防止多模态字段丢失。
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setConversationId(conversationId);
        chatMessage.setRole(message != null ? message.getRole() : "user");
        chatMessage.setContentType(firstContentType(message));
        chatMessage.setContent(flattenText(message));
        chatMessage.setRawContent(flattenRaw(message));
        chatMessage.setModel(model);
        messageMapper.insertChatMessage(chatMessage);
        conversationMapper.touchChatConversation(conversationId);
        return chatMessage;
    }

    @Override
    @Transactional
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
        conversationMapper.touchChatConversation(conversationId);
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
    public List<ChatMessage> listRecentMessages(String conversationId, int limit) {
        return messageMapper.selectRecentChatMessages(conversationId, Math.max(1, Math.min(limit, 100)));
    }

    @Override
    @Transactional
    public int deleteConversation(String conversationId) {
        // 先删子表消息再删父表会话，既符合外键关系，也保证两个操作处于同一数据库事务。
        messageMapper.deleteChatMessagesByConversationId(conversationId);
        return conversationMapper.deleteChatConversationById(conversationId);
    }

    private static String buildTitle(ChatRequest request) {
        // 标题只取第一条用户文本并限制长度，避免整个长问题出现在会话列表。
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
