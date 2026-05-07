package com.tyh.chat.rag.log.service;

import com.tyh.chat.rag.log.domain.RagQueryLog;

import java.util.List;

public interface RagQueryLogService {

    void record(RagQueryLog log);

    List<RagQueryLog> list(RagQueryLog query);
}
