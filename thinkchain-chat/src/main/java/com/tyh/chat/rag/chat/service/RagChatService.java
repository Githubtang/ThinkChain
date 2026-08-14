package com.tyh.chat.rag.chat.service;

import com.tyh.chat.rag.chat.dto.RagChatRequest;
import com.tyh.chat.rag.chat.dto.RagChatResponse;

/**
 * 独立 RAG 问答接口。
 *
 * <p>RAG 的含义是“先从用户选择的资料中检索相关片段，再把片段连同问题交给聊天模型”。
 * 这样模型回答时可以参考项目自己的知识库或当前会话上传的文档。</p>
 */
public interface RagChatService {

    /**
     * 执行一次完整的“问题向量化 → 相似度检索 → 构造提示词 → 调用模型”流程。
     *
     * @param request RAG 问答请求，必须包含模型、问题以及与 ragMode 对应的资料范围
     * @return 回答正文、普通对话结果、引用来源和总耗时
     */
    RagChatResponse chat(RagChatRequest request);
}
