package com.tyh.chat.rag.session.service;

import com.tyh.chat.rag.session.domain.SessionDocument;

import java.util.List;

public interface SessionDocumentService {

    SessionDocument getById(String id);

    List<SessionDocument> list(SessionDocument query);

    int create(SessionDocument document);

    int update(SessionDocument document);

    int deleteById(String id);
}
