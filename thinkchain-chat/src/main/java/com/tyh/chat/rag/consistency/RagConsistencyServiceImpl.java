package com.tyh.chat.rag.consistency;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.embedding.service.RagEmbeddingService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.exception.ServiceException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * RAG 向量一致性检查和修复实现。
 *
 * <p>修复采用稳定 chunkId：缺失向量重新写入时会触发 pgvector 的 upsert，不会产生重复记录；
 * 已失去主库切片的向量则直接删除。</p>
 */
@Service
public class RagConsistencyServiceImpl implements RagConsistencyService {

    private final KnowledgeChunkService chunkService;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;
    private final RagEmbeddingService embeddingService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final SessionDocumentService sessionDocumentService;

    public RagConsistencyServiceImpl(KnowledgeChunkService chunkService,
                                     ObjectProvider<RagEmbeddingStore> embeddingStoreProvider,
                                     RagEmbeddingService embeddingService,
                                     KnowledgeDocumentService knowledgeDocumentService,
                                     SessionDocumentService sessionDocumentService) {
        this.chunkService = chunkService;
        this.embeddingStoreProvider = embeddingStoreProvider;
        this.embeddingService = embeddingService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.sessionDocumentService = sessionDocumentService;
    }

    @Override
    public RagConsistencyReport inspect(String documentId) {
        RagEmbeddingStore store = requiredStore();
        List<KnowledgeChunk> chunks = chunks(documentId);
        Set<String> chunkIds = new LinkedHashSet<>(chunks.stream().map(KnowledgeChunk::getId).toList());
        Set<String> vectorIds = new LinkedHashSet<>(store.findChunkIdsByDocumentId(documentId));

        List<String> missing = chunkIds.stream().filter(id -> !vectorIds.contains(id)).toList();
        List<String> orphan = vectorIds.stream().filter(id -> !chunkIds.contains(id)).toList();
        RagConsistencyReport report = new RagConsistencyReport();
        report.setDocumentId(documentId);
        report.setChunkCount(chunkIds.size());
        report.setVectorCount(vectorIds.size());
        report.setMissingChunkIds(missing);
        report.setOrphanVectorIds(orphan);
        report.setConsistent(!chunkIds.isEmpty() && missing.isEmpty() && orphan.isEmpty());
        return report;
    }

    @Override
    public RagConsistencyReport repair(String documentId) {
        RagEmbeddingStore store = requiredStore();
        RagConsistencyReport before = inspect(documentId);
        for (String vectorId : before.getOrphanVectorIds()) {
            store.deleteByChunkId(vectorId);
        }
        if (!before.getMissingChunkIds().isEmpty()) {
            Set<String> missing = Set.copyOf(before.getMissingChunkIds());
            for (KnowledgeChunk chunk : chunks(documentId)) {
                if (missing.contains(chunk.getId())) {
                    // EmbeddingService 会处理 FAILED 切片，并用 chunkId 幂等写入向量表。
                    chunk.setEmbeddingStatus("FAILED");
                    chunkService.update(chunk);
                }
            }
            embeddingService.embedDocument(documentId);
        }

        RagConsistencyReport after = inspect(documentId);
        after.setRepaired(true);
        updateDocumentStatus(documentId, after.isConsistent());
        return after;
    }

    private RagEmbeddingStore requiredStore() {
        RagEmbeddingStore store = embeddingStoreProvider.getIfAvailable();
        if (store == null) {
            throw new ServiceException("向量数据库不可用", HttpStatus.ERROR);
        }
        return store;
    }

    private List<KnowledgeChunk> chunks(String documentId) {
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        return chunkService.list(query);
    }

    private void updateDocumentStatus(String documentId, boolean consistent) {
        Date finishedAt = new Date();
        String error = consistent ? null : "向量一致性修复后仍存在缺失数据";
        KnowledgeDocument knowledgeDocument = knowledgeDocumentService.getById(documentId);
        if (knowledgeDocument != null) {
            knowledgeDocument.setStatus(consistent ? "READY" : "FAILED");
            knowledgeDocument.setErrorMessage(error);
            knowledgeDocument.setProcessingFinishedAt(finishedAt);
            knowledgeDocumentService.update(knowledgeDocument);
            return;
        }
        SessionDocument sessionDocument = sessionDocumentService.getById(documentId);
        if (sessionDocument != null) {
            sessionDocument.setParseStatus(consistent ? "READY" : "FAILED");
            sessionDocument.setErrorMessage(error);
            sessionDocument.setProcessingFinishedAt(finishedAt);
            sessionDocumentService.update(sessionDocument);
        }
    }
}
