package com.tyh.chat.rag.document.service;

import com.tyh.chat.rag.document.domain.KnowledgeDocument;

import java.util.List;

public interface KnowledgeDocumentService {

    KnowledgeDocument getById(String id);

    List<KnowledgeDocument> list(KnowledgeDocument query);

    int create(KnowledgeDocument document);

    int update(KnowledgeDocument document);

    int deleteById(String id);
}
