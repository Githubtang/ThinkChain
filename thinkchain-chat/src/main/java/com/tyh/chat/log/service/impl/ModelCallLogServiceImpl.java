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
/**
 * 模型调用日志的 MyBatis 实现。
 *
 * <p>这里只负责日志记录本身；请求 JSON 的脱敏和截断在 AiChatService 写入之前完成。</p>
 */
@Service
public class ModelCallLogServiceImpl implements ModelCallLogService {

    private final ModelCallLogMapper mapper;

    public ModelCallLogServiceImpl(ModelCallLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(ModelCallLog log) {
        // 日志属于附属数据，但仍保证每条记录有唯一 ID 和明确状态，方便后续检索统计。
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
