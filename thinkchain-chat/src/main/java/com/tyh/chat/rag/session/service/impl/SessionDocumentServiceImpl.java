package com.tyh.chat.rag.session.service.impl;

import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.mapper.SessionDocumentMapper;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
        if (document.getId() == null || document.getId().isBlank()) {
            document.setId(UUID.randomUUID().toString());
        }
        if (document.getParseStatus() == null || document.getParseStatus().isBlank()) {
            document.setParseStatus("PENDING");
        }
        if (document.getChunkCount() == null) {
            document.setChunkCount(0);
        }
        return mapper.insertSessionDocument(document);
    }

    @Override
    public int update(SessionDocument document) {
        return mapper.updateSessionDocument(document);
    }

    @Override
    public int deleteById(String id) {
        return mapper.deleteSessionDocumentById(id);
    }
}
