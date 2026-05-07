package com.tyh.chat.rag.chunk.service;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;

import java.util.List;

public interface KnowledgeChunkService {

    KnowledgeChunk getById(String id);

    List<KnowledgeChunk> list(KnowledgeChunk query);

    int create(KnowledgeChunk chunk);

    int update(KnowledgeChunk chunk);

    int deleteByDocumentId(String documentId);
}
