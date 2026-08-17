package com.tyh.chat.rag.embedding.service.impl;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.embedding.client.EmbeddingClient;
import com.tyh.chat.rag.embedding.service.RagEmbeddingService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingRecord;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档切片向量化的业务实现。
 *
 * <p>它从主业务数据库读取状态为 PENDING 的切片，调用 EmbeddingClient 得到浮点向量，
 * 再通过 RagEmbeddingStore 写入 Supabase PostgreSQL 的 pgvector 表，最后更新切片处理状态。</p>
 */
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
        // 向量数据源是条件配置；未创建存储实现时给出明确错误，而不是空指针。
        RagEmbeddingStore embeddingStore = embeddingStoreProvider.getIfAvailable();
        if (embeddingStore == null) {
            throw new IllegalStateException("RagEmbeddingStore is unavailable; check vector datasource config");
        }
        // 同时处理首次任务 PENDING 和上次失败的 FAILED；COMPLETED 不重复调用收费接口。
        List<KnowledgeChunk> chunks = new ArrayList<>();
        chunks.addAll(listByStatus(documentId, "PENDING"));
        chunks.addAll(listByStatus(documentId, "FAILED"));
        int successCount = 0;
        for (KnowledgeChunk chunk : chunks) {
            try {
                // 使用 chunkId 作为稳定 vectorId，重试时执行更新而不是插入重复向量。
                float[] embedding = embeddingClient.embed(chunk.getContent());
                String vectorId = chunk.getId();
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
                // 只有向量库写入成功后，主库切片状态才标记为 COMPLETED。
                chunk.setVectorId(vectorId);
                chunk.setEmbeddingStatus("COMPLETED");
                chunkService.update(chunk);
                successCount++;
            } catch (Exception e) {
                // 单个切片失败不终止整个文档，其余切片仍可继续处理；FAILED 切片可后续重试。
                chunk.setEmbeddingStatus("FAILED");
                chunkService.update(chunk);
            }
        }
        return successCount;
    }

    /** 查询一个文档下指定向量化状态的切片。 */
    private List<KnowledgeChunk> listByStatus(String documentId, String status) {
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        query.setEmbeddingStatus(status);
        return chunkService.list(query);
    }
}
