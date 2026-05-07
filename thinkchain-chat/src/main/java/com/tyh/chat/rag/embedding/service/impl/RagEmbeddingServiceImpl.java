package com.tyh.chat.rag.embedding.service.impl;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.embedding.client.EmbeddingClient;
import com.tyh.chat.rag.embedding.service.RagEmbeddingService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingRecord;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RagEmbeddingServiceImpl implements RagEmbeddingService {

    private final KnowledgeChunkService chunkService;
    private final EmbeddingClient embeddingClient;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;

    public RagEmbeddingServiceImpl(KnowledgeChunkService chunkService,
                                   EmbeddingClient embeddingClient,
                                   ObjectProvider<RagEmbeddingStore> embeddingStoreProvider) {
        this.chunkService = chunkService;
        this.embeddingClient = embeddingClient;
        this.embeddingStoreProvider = embeddingStoreProvider;
    }

    @Override
    public int embedDocument(String documentId) {
        RagEmbeddingStore embeddingStore = embeddingStoreProvider.getIfAvailable();
        if (embeddingStore == null) {
            throw new IllegalStateException("RagEmbeddingStore is unavailable; check vector datasource config");
        }
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        query.setEmbeddingStatus("PENDING");
        List<KnowledgeChunk> chunks = chunkService.list(query);
        int successCount = 0;
        for (KnowledgeChunk chunk : chunks) {
            try {
                float[] embedding = embeddingClient.embed(chunk.getContent());
                String vectorId = UUID.randomUUID().toString();
                RagEmbeddingRecord record = new RagEmbeddingRecord();
                record.setId(vectorId);
                record.setScopeType(chunk.getScopeType());
                record.setScopeId(chunk.getScopeId());
                record.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
                record.setConversationId(chunk.getConversationId());
                record.setDocumentId(chunk.getDocumentId());
                record.setChunkId(chunk.getId());
                record.setEmbeddingModel(embeddingClient.modelName());
                record.setContent(chunk.getContent());
                record.setEmbedding(embedding);
                record.setMetadata("{}");
                embeddingStore.save(record);
                chunk.setVectorId(vectorId);
                chunk.setEmbeddingStatus("COMPLETED");
                chunkService.update(chunk);
                successCount++;
            } catch (Exception e) {
                chunk.setEmbeddingStatus("FAILED");
                chunkService.update(chunk);
            }
        }
        return successCount;
    }
}
