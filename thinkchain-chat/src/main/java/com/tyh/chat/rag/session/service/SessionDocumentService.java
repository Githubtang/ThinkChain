package com.tyh.chat.rag.session.service;

import com.tyh.chat.rag.session.domain.SessionDocument;

import java.util.List;

/** 会话临时文档元数据服务，记录文件归属会话、保存路径、解析状态和切片数量。 */
public interface SessionDocumentService {

    /** 根据文档 ID 查询；不存在时返回 null。 */
    SessionDocument getById(String id);

    /** 根据会话、用户、解析状态等非空字段筛选文档。 */
    List<SessionDocument> list(SessionDocument query);

    /** 创建临时文档元数据并补充 ID、默认状态和默认切片数。 */
    int create(SessionDocument document);

    /** 更新临时文档元数据或解析状态。 */
    int update(SessionDocument document);

    /** 只删除元数据；完整清理向量、切片和文件时调用 ChatResourceDeletionService。 */
    int deleteById(String id);
}
