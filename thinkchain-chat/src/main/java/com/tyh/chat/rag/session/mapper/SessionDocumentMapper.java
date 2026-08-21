package com.tyh.chat.rag.session.mapper;

import com.tyh.chat.rag.session.domain.SessionDocument;

import java.util.List;

public interface SessionDocumentMapper {

    SessionDocument selectSessionDocumentById(String id);

    List<SessionDocument> selectSessionDocumentList(SessionDocument document);

    int insertSessionDocument(SessionDocument document);

    int updateSessionDocument(SessionDocument document);

    int claimSessionDocumentProcessing(String id);

    int requestSessionDocumentProcessing(String id);

    int resetInterruptedSessionDocumentProcessing();

    int deleteSessionDocumentById(String id);
}
