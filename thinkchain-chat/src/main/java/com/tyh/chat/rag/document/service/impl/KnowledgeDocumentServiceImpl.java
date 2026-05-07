package com.tyh.chat.rag.document.service.impl;

import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.mapper.KnowledgeDocumentMapper;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    private final KnowledgeDocumentMapper mapper;

    public KnowledgeDocumentServiceImpl(KnowledgeDocumentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public KnowledgeDocument getById(String id) {
        return mapper.selectKnowledgeDocumentById(id);
    }

    @Override
    public List<KnowledgeDocument> list(KnowledgeDocument query) {
        return mapper.selectKnowledgeDocumentList(query);
    }

    @Override
    public int create(KnowledgeDocument document) {
        if (document.getId() == null || document.getId().isBlank()) {
            document.setId(UUID.randomUUID().toString());
        }
        if (document.getStatus() == null || document.getStatus().isBlank()) {
            document.setStatus("UPLOADED");
        }
        if (document.getChunkCount() == null) {
            document.setChunkCount(0);
        }
        return mapper.insertKnowledgeDocument(document);
    }

    @Override
    public int update(KnowledgeDocument document) {
        return mapper.updateKnowledgeDocument(document);
    }

    @Override
    public int deleteById(String id) {
        return mapper.deleteKnowledgeDocumentById(id);
    }
}
