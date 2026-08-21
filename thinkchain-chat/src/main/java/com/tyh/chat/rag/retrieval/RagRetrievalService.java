package com.tyh.chat.rag.retrieval;

import com.tyh.chat.rag.embedding.store.RagEmbeddingMatch;

import java.util.List;

/**
 * RAG 统一检索接口。
 *
 * <p>本接口只完成“问题向量化、按资料范围查询、去重、排序和阈值过滤”，不调用聊天模型，
 * 也不拼装 HTTP 响应。普通聊天和独立 RAG 问答必须共同调用它，保证召回结果一致。</p>
 */
public interface RagRetrievalService {

    /**
     * 检索与问题相关的文档切片。
     *
     * @param question           当前用户问题
     * @param conversationId     会话 ID，用于限定 SESSION 文档范围
     * @param knowledgeBaseIds   本次允许检索的知识库 ID
     * @param sessionDocumentIds 本次允许检索的会话文档 ID
     * @param ragMode            AUTO、SESSION_ONLY、KB_ONLY、SESSION_AND_KB 或 NONE
     * @param requestedTopK      请求指定的召回数，为空时使用配置默认值
     * @param requestedMinScore  请求指定的最低相似度，为空时使用配置默认值
     * @return 已去重、过滤并按统一规则排序的切片；没有可靠命中时返回空列表
     */
    List<RagEmbeddingMatch> retrieve(String question,
                                     String conversationId,
                                     List<String> knowledgeBaseIds,
                                     List<String> sessionDocumentIds,
                                     String ragMode,
                                     Integer requestedTopK,
                                     Double requestedMinScore);

    /** 返回本次实际采用的 topK，供提示词和日志使用。 */
    int effectiveTopK(Integer requestedTopK);
}
