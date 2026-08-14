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
/**
 * 模型调用审计日志服务。
 *
 * <p>记录一次模型请求使用了哪个模型、是否成功、耗时和脱敏后的请求/响应。
 * 它用于排查问题和统计，不参与模型回答内容的生成。</p>
 */
public interface ModelCallLogService {

    /** 保存一条调用日志；未设置 ID 时由实现类自动生成。 */
    void record(ModelCallLog log);

    /** 根据非空查询字段筛选调用日志。 */
    List<ModelCallLog> list(ModelCallLog query);
}
