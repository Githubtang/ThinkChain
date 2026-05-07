package com.tyh.chat.rag.chunk.mapper;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;

import java.util.List;

public interface KnowledgeChunkMapper {

    KnowledgeChunk selectKnowledgeChunkById(String id);

    List<KnowledgeChunk> selectKnowledgeChunkList(KnowledgeChunk chunk);

    int insertKnowledgeChunk(KnowledgeChunk chunk);

    int updateKnowledgeChunk(KnowledgeChunk chunk);

    int deleteKnowledgeChunksByDocumentId(String documentId);
}
