package com.tyh.chat.rag.log.service;

import com.tyh.chat.rag.log.domain.RagQueryLog;

import java.util.List;

/** RAG 查询审计日志服务，记录问题、检索范围、命中切片、回答、耗时和失败原因。 */
public interface RagQueryLogService {

    /** 保存一次 RAG 查询日志；ID 或状态为空时由实现类补默认值。 */
    void record(RagQueryLog log);

    /** 根据当前用户和其他非空条件筛选 RAG 日志。 */
    List<RagQueryLog> list(RagQueryLog query);
}
