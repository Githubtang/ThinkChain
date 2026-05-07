package com.tyh.chat.rag.knowledge.service;

import com.tyh.chat.rag.knowledge.domain.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService {

    KnowledgeBase getById(String id);

    List<KnowledgeBase> list(KnowledgeBase query);

    int create(KnowledgeBase knowledgeBase);

    int update(KnowledgeBase knowledgeBase);

    int deleteById(String id);
}
