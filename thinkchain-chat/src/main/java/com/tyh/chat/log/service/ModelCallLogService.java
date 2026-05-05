package com.tyh.chat.log.service;

import com.tyh.chat.log.domain.ModelCallLog;

import java.util.List;

/**
 * 模型调用日志服务。
 *
 * @Author: GithubTang
 * @Description: 模型调用日志服务
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public interface ModelCallLogService {

    void record(ModelCallLog log);

    List<ModelCallLog> list(ModelCallLog query);
}
