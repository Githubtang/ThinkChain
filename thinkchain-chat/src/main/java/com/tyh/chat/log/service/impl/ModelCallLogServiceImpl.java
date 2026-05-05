package com.tyh.chat.log.service.impl;

import com.tyh.chat.log.domain.ModelCallLog;
import com.tyh.chat.log.mapper.ModelCallLogMapper;
import com.tyh.chat.log.service.ModelCallLogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 模型调用日志服务实现。
 *
 * @Author: GithubTang
 * @Description: 模型调用日志服务实现
 * @Date: 2026/4/29
 * @Version: 1.0
 */
@Service
public class ModelCallLogServiceImpl implements ModelCallLogService {

    private final ModelCallLogMapper mapper;

    public ModelCallLogServiceImpl(ModelCallLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(ModelCallLog log) {
        if (log.getId() == null || log.getId().isBlank()) {
            log.setId(UUID.randomUUID().toString());
        }
        mapper.insertModelCallLog(log);
    }

    @Override
    public List<ModelCallLog> list(ModelCallLog query) {
        return mapper.selectModelCallLogList(query);
    }
}
