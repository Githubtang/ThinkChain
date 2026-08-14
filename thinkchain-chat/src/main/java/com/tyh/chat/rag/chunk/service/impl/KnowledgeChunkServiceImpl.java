package com.tyh.chat.rag.chunk.service.impl;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.mapper.KnowledgeChunkMapper;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 文档切片的 MyBatis 实现。
 *
 * <p>知识库文档和会话文档共用同一套切片表，通过 scopeType、knowledgeBaseId、conversationId 区分作用域。</p>
 */
@Service
public class KnowledgeChunkServiceImpl implements KnowledgeChunkService {

    private final KnowledgeChunkMapper mapper;

    public KnowledgeChunkServiceImpl(KnowledgeChunkMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public KnowledgeChunk getById(String id) {
        return mapper.selectKnowledgeChunkById(id);
    }

    @Override
    public List<KnowledgeChunk> list(KnowledgeChunk query) {
        return mapper.selectKnowledgeChunkList(query);
    }

    @Override
    public int create(KnowledgeChunk chunk) {
        // 解析阶段先创建 PENDING 切片，成功写入向量库后再更新为 EMBEDDED。
        if (chunk.getId() == null || chunk.getId().isBlank()) {
            chunk.setId(UUID.randomUUID().toString());
        }
        if (chunk.getScopeType() == null || chunk.getScopeType().isBlank()) {
            chunk.setScopeType(chunk.getConversationId() != null && !chunk.getConversationId().isBlank() ? "SESSION" : "KB");
        }
        if (chunk.getScopeId() == null || chunk.getScopeId().isBlank()) {
            chunk.setScopeId("SESSION".equalsIgnoreCase(chunk.getScopeType()) ? chunk.getConversationId() : chunk.getKnowledgeBaseId());
        }
        if (chunk.getEmbeddingStatus() == null || chunk.getEmbeddingStatus().isBlank()) {
            chunk.setEmbeddingStatus("PENDING");
        }
        return mapper.insertKnowledgeChunk(chunk);
    }

    @Override
    public int update(KnowledgeChunk chunk) {
        return mapper.updateKnowledgeChunk(chunk);
    }

    @Override
    public int deleteByDocumentId(String documentId) {
        return mapper.deleteKnowledgeChunksByDocumentId(documentId);
    }
}
