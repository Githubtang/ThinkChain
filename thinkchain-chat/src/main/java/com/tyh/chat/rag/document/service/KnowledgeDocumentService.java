package com.tyh.chat.rag.document.service;

import com.tyh.chat.rag.document.domain.KnowledgeDocument;

import java.util.List;

/** 知识库文档元数据服务，管理文件名、路径、解析状态、切片数量等信息。 */
public interface KnowledgeDocumentService {

    /** 根据文档 ID 查询元数据；不存在时返回 null。 */
    KnowledgeDocument getById(String id);

    /** 根据知识库 ID、用户 ID、状态等非空字段筛选文档。 */
    List<KnowledgeDocument> list(KnowledgeDocument query);

    /** 新增文档元数据，并补充 ID、默认状态和默认切片数。 */
    int create(KnowledgeDocument document);

    /** 更新文档元数据或处理状态。 */
    int update(KnowledgeDocument document);

    /** 仅当状态允许时抢占后台任务，返回 1 表示本线程取得处理权。 */
    int claimProcessing(String documentId);

    /** 用户主动重新处理时，把 READY/FAILED 文档改回 PENDING。 */
    int requestProcessing(String documentId);

    /** 应用启动时把上次异常退出留下的处理中状态恢复为 PENDING。 */
    int resetInterruptedProcessing();

    /** 只删除文档元数据；完整删除流程应优先调用 ChatResourceDeletionService。 */
    int deleteById(String id);
}
