package com.tyh.chat.rag.log.service.impl;

import com.tyh.chat.rag.log.domain.RagQueryLog;
import com.tyh.chat.rag.log.mapper.RagQueryLogMapper;
import com.tyh.chat.rag.log.service.RagQueryLogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RagQueryLogServiceImpl implements RagQueryLogService {

    private final RagQueryLogMapper mapper;

    public RagQueryLogServiceImpl(RagQueryLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(RagQueryLog log) {
        if (log.getId() == null || log.getId().isBlank()) {
            log.setId(UUID.randomUUID().toString());
        }
        mapper.insertRagQueryLog(log);
    }

    @Override
    public List<RagQueryLog> list(RagQueryLog query) {
        return mapper.selectRagQueryLogList(query);
    }
}
