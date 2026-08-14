package com.tyh.chat.rag.knowledge.service.impl;

import com.tyh.chat.rag.knowledge.domain.KnowledgeBase;
import com.tyh.chat.rag.knowledge.mapper.KnowledgeBaseMapper;
import com.tyh.chat.rag.knowledge.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 知识库元数据的 MyBatis 实现。
 *
 * <p>知识库本身不保存文件正文，只保存名称、说明、状态和统计数量；具体文档由 KnowledgeDocumentService 管理。</p>
 */
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper mapper;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public KnowledgeBase getById(String id) {
        return mapper.selectKnowledgeBaseById(id);
    }

    @Override
    public List<KnowledgeBase> list(KnowledgeBase query) {
        return mapper.selectKnowledgeBaseList(query);
    }

    @Override
    public int create(KnowledgeBase knowledgeBase) {
        // 控制器通常不传这些系统字段，因此在服务层统一生成，避免不同入口产生不同默认值。
        if (knowledgeBase.getId() == null || knowledgeBase.getId().isBlank()) {
            knowledgeBase.setId(UUID.randomUUID().toString());
        }
        if (knowledgeBase.getStatus() == null || knowledgeBase.getStatus().isBlank()) {
            knowledgeBase.setStatus("ACTIVE");
        }
        if (knowledgeBase.getDocumentCount() == null) {
            knowledgeBase.setDocumentCount(0);
        }
        if (knowledgeBase.getChunkCount() == null) {
            knowledgeBase.setChunkCount(0);
        }
        return mapper.insertKnowledgeBase(knowledgeBase);
    }

    @Override
    public int update(KnowledgeBase knowledgeBase) {
        return mapper.updateKnowledgeBase(knowledgeBase);
    }

    @Override
    public int deleteById(String id) {
        return mapper.deleteKnowledgeBaseById(id);
    }
}
