package com.tyh.chat.conversation.mapper;

import com.tyh.chat.conversation.domain.ChatConversation;

import java.util.List;

/**
 * 对话会话数据映射接口。
 *
 * @Author: GithubTang
 * @Description: 会话映射器
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public interface ChatConversationMapper {

    ChatConversation selectChatConversationById(String id);

    List<ChatConversation> selectChatConversationList(ChatConversation conversation);

    int insertChatConversation(ChatConversation conversation);

    int updateChatConversation(ChatConversation conversation);

    int deleteChatConversationById(String id);
}
