package com.tyh.chat.rag.embedding.store;

import java.util.List;

public interface RagEmbeddingStore {

    int save(RagEmbeddingRecord record);

    int deleteByChunkId(String chunkId);

    List<RagEmbeddingMatch> search(String knowledgeBaseId, float[] queryEmbedding, int topK);
}
