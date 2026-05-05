package com.tyh.chat.conversation.mapper;

import com.tyh.chat.conversation.domain.ChatMessage;

import java.util.List;

/**
 * 对话消息数据映射接口。
 *
 * @Author: GithubTang
 * @Description: 消息映射器
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public interface ChatMessageMapper {

    ChatMessage selectChatMessageById(String id);

    List<ChatMessage> selectChatMessageList(ChatMessage message);

    int insertChatMessage(ChatMessage message);

    int deleteChatMessagesByConversationId(String conversationId);
}
