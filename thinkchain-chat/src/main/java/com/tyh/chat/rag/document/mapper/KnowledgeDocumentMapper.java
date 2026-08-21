package com.tyh.chat.rag.document.mapper;

import com.tyh.chat.rag.document.domain.KnowledgeDocument;

import java.util.List;

public interface KnowledgeDocumentMapper {

    KnowledgeDocument selectKnowledgeDocumentById(String id);

    List<KnowledgeDocument> selectKnowledgeDocumentList(KnowledgeDocument document);

    int insertKnowledgeDocument(KnowledgeDocument document);

    int updateKnowledgeDocument(KnowledgeDocument document);

    int claimKnowledgeDocumentProcessing(String id);

    int requestKnowledgeDocumentProcessing(String id);

    int resetInterruptedKnowledgeDocumentProcessing();

    int deleteKnowledgeDocumentById(String id);
}
