package com.tyh.chat.rag.embedding.store;

import java.util.List;

public interface RagEmbeddingStore {

    int save(RagEmbeddingRecord record);

    int deleteByChunkId(String chunkId);

    int deleteByDocumentId(String documentId);

    List<RagEmbeddingMatch> search(String knowledgeBaseId, float[] queryEmbedding, int topK);

    List<RagEmbeddingMatch> searchByScope(String scopeType, String scopeId, float[] queryEmbedding, int topK);

    List<RagEmbeddingMatch> searchByScope(String scopeType, String scopeId, List<String> documentIds,
                                          float[] queryEmbedding, int topK);
}
