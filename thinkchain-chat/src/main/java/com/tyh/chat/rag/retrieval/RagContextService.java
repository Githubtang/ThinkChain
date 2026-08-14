package com.tyh.chat.rag.retrieval;

import com.tyh.chat.chat.dto.ChatRequest;

/**
 * 普通聊天接口中的可选 RAG 增强服务。
 *
 * <p>它不会直接调用聊天模型，而是根据 ChatRequest 中选择的资料执行检索，
 * 然后返回一个增加了系统参考资料的新请求。</p>
 */
public interface RagContextService {

    /**
     * 在满足 RAG 条件时为请求增加检索上下文；未开启、无向量库或无命中时原样返回。
     */
    ChatRequest augment(ChatRequest request);
}
