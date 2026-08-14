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
/**
 * 对话会话和消息的持久化接口。
 *
 * <p>“会话”保存标题、模型等总体信息；“消息”保存每一次用户输入和模型回复。
 * 该接口只负责数据持久化，不负责登录用户鉴权，资源归属校验由 ChatAccessService 在进入这里之前完成。</p>
 */
public interface ConversationService {

    /** 根据会话 ID 查询会话；不存在时返回 null。 */
    ChatConversation getConversation(String conversationId);

    /**
     * 确保本次请求存在会话。
     * 请求有 conversationId 时查询原会话，没有时创建新会话并把新 ID 回写到 request。
     */
    ChatConversation ensureConversation(ChatRequest request);

    /** 保存一条用户消息，并把多段内容整理成便于查询的文本和原始 JSON。 */
    ChatMessage saveUserMessage(String conversationId, String model, Message message);

    /** 保存模型回复；rawContent 用于保留厂商原始响应，content 是展示给用户的正文。 */
    ChatMessage saveAssistantMessage(String conversationId, String model, String content, String rawContent);

    /** 查询指定用户自己的全部会话，通常按最近更新时间倒序返回。 */
    List<ChatConversation> listConversations(String userId);

    /** 查询一个会话中的全部消息；调用前必须先完成会话归属校验。 */
    List<ChatMessage> listMessages(String conversationId);

    /** 删除会话及其消息，返回受影响的会话记录数。 */
    int deleteConversation(String conversationId);
}
