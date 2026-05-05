package com.tyh.chat.log.mapper;

import com.tyh.chat.log.domain.ModelCallLog;

import java.util.List;

/**
 * 模型调用日志数据映射接口。
 *
 * @Author: GithubTang
 * @Description: 模型调用日志映射器
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public interface ModelCallLogMapper {

    int insertModelCallLog(ModelCallLog log);

    List<ModelCallLog> selectModelCallLogList(ModelCallLog log);
}
