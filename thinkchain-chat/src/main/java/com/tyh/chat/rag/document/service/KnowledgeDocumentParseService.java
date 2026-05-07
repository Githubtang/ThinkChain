package com.tyh.chat.rag.document.service;

import com.tyh.chat.rag.document.domain.KnowledgeDocument;

public interface KnowledgeDocumentParseService {

    KnowledgeDocument parse(String documentId);
}
