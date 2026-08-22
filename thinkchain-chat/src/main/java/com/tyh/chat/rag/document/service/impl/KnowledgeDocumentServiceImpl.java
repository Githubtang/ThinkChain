package com.tyh.chat.rag.document.service.impl;

import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.mapper.KnowledgeDocumentMapper;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 知识库文档元数据的 MyBatis 实现。
 *
 * <p>本类不读取磁盘文件，也不生成切片；文件解析由 KnowledgeDocumentParseServiceImpl 完成。</p>
 */
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
        // 新上传文档尚未解析，因此默认是 UPLOADED，切片数量从 0 开始。
        if (document.getId() == null || document.getId().isBlank()) {
            document.setId(UUID.randomUUID().toString());
        }
        if (document.getStatus() == null || document.getStatus().isBlank()) {
            document.setStatus("UPLOADED");
        }
        if (document.getChunkCount() == null) {
            document.setChunkCount(0);
        }
        if (document.getRetryCount() == null) {
            document.setRetryCount(0);
        }
        return mapper.insertKnowledgeDocument(document);
    }

    @Override
    public int update(KnowledgeDocument document) {
        return mapper.updateKnowledgeDocument(document);
    }

    @Override
    public int claimProcessing(String documentId) {
        return mapper.claimKnowledgeDocumentProcessing(documentId);
    }

    @Override
    public int requestProcessing(String documentId) {
        return mapper.requestKnowledgeDocumentProcessing(documentId);
    }

    @Override
    public int resetInterruptedProcessing() {
        return mapper.resetInterruptedKnowledgeDocumentProcessing();
    }

    @Override
    public int deleteById(String id) {
        return mapper.deleteKnowledgeDocumentById(id);
    }
}
