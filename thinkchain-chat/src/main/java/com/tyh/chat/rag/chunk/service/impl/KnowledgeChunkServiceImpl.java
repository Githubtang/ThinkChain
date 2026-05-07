package com.tyh.chat.rag.chunk.service.impl;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.mapper.KnowledgeChunkMapper;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
