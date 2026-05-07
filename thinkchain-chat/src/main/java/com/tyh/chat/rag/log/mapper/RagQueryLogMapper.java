package com.tyh.chat.rag.log.mapper;

import com.tyh.chat.rag.log.domain.RagQueryLog;

import java.util.List;

public interface RagQueryLogMapper {

    int insertRagQueryLog(RagQueryLog log);

    List<RagQueryLog> selectRagQueryLogList(RagQueryLog log);
}
