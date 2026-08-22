package com.tyh.chat.rag.session.service.impl;

import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.mapper.SessionDocumentMapper;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 会话临时文档元数据的 MyBatis 实现。
 *
 * <p>与知识库文档不同，这类文档绑定 conversationId，只应在所属会话的 RAG 检索中使用。</p>
 */
@Service
public class SessionDocumentServiceImpl implements SessionDocumentService {

    private final SessionDocumentMapper mapper;

    public SessionDocumentServiceImpl(SessionDocumentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public SessionDocument getById(String id) {
        return mapper.selectSessionDocumentById(id);
    }

    @Override
    public List<SessionDocument> list(SessionDocument query) {
        return mapper.selectSessionDocumentList(query);
    }

    @Override
    public int create(SessionDocument document) {
        // PENDING 表示文件已登记但尚未成功完成解析。
        if (document.getId() == null || document.getId().isBlank()) {
            document.setId(UUID.randomUUID().toString());
        }
        if (document.getParseStatus() == null || document.getParseStatus().isBlank()) {
            document.setParseStatus("PENDING");
        }
        if (document.getChunkCount() == null) {
            document.setChunkCount(0);
        }
        if (document.getRetryCount() == null) {
            document.setRetryCount(0);
        }
        return mapper.insertSessionDocument(document);
    }

    @Override
    public int update(SessionDocument document) {
        return mapper.updateSessionDocument(document);
    }

    @Override
    public int claimProcessing(String documentId) {
        return mapper.claimSessionDocumentProcessing(documentId);
    }

    @Override
    public int requestProcessing(String documentId) {
        return mapper.requestSessionDocumentProcessing(documentId);
    }

    @Override
    public int resetInterruptedProcessing() {
        return mapper.resetInterruptedSessionDocumentProcessing();
    }

    @Override
    public int deleteById(String id) {
        return mapper.deleteSessionDocumentById(id);
    }
}
