package com.tyh.chat.rag.knowledge.mapper;

import com.tyh.chat.rag.knowledge.domain.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseMapper {

    KnowledgeBase selectKnowledgeBaseById(String id);

    List<KnowledgeBase> selectKnowledgeBaseList(KnowledgeBase knowledgeBase);

    int insertKnowledgeBase(KnowledgeBase knowledgeBase);

    int updateKnowledgeBase(KnowledgeBase knowledgeBase);

    int deleteKnowledgeBaseById(String id);
}
