package com.tyh.chat.rag.embedding.store;

import java.util.List;

/**
 * RAG 向量数据的存储与检索接口。
 *
 * <p>接口隔离了 PostgreSQL/pgvector 的 SQL 细节。业务层只关心保存、删除和相似度查询，
 * 当前实现是 JdbcRagEmbeddingStore。</p>
 */
public interface RagEmbeddingStore {

    /** 新增或更新一条切片向量，返回数据库影响行数。 */
    int save(RagEmbeddingRecord record);

    /** 根据切片 ID 删除向量。 */
    int deleteByChunkId(String chunkId);

    /** 根据文档 ID 删除其全部向量。 */
    int deleteByDocumentId(String documentId);

    /** 查询一个文档在向量库中实际存在的切片 ID，用于主库与 Supabase 一致性检查。 */
    List<String> findChunkIdsByDocumentId(String documentId);

    /** 在单个知识库中查询与问题最相似的前 topK 个切片。 */
    List<RagEmbeddingMatch> search(String knowledgeBaseId, float[] queryEmbedding, int topK);

    /** 在指定作用域中检索；scopeType 为 KB 或 SESSION，scopeId 是知识库或会话 ID。 */
    List<RagEmbeddingMatch> searchByScope(String scopeType, String scopeId, float[] queryEmbedding, int topK);

    /**
     * 在指定作用域和指定文档集合内检索，主要用于限制只搜索用户本次选中的会话文档。
     */
    List<RagEmbeddingMatch> searchByScope(String scopeType, String scopeId, List<String> documentIds,
                                          float[] queryEmbedding, int topK);
}
