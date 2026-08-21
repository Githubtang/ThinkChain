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

    /** 仅当状态允许时抢占后台任务，返回 1 表示本线程取得处理权。 */
    int claimProcessing(String documentId);

    /** 用户主动重新处理时，把 READY/FAILED 文档改回 PENDING。 */
    int requestProcessing(String documentId);

    /** 应用启动时把上次异常退出留下的处理中状态恢复为 PENDING。 */
    int resetInterruptedProcessing();

    /** 只删除元数据；完整清理向量、切片和文件时调用 ChatResourceDeletionService。 */
    int deleteById(String id);
}
