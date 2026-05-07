package com.tyh.chat.rag.session.service;

import com.tyh.chat.rag.session.domain.SessionDocument;

public interface SessionDocumentParseService {

    SessionDocument parse(String documentId);
}
