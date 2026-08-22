package com.tyh.chat.rag.processing;

/**
 * 文档后台处理接口。
 *
 * <p>Controller 只负责把任务放入队列并立即返回文档状态；本服务在后台依次执行解析、切片和向量化。</p>
 */
public interface DocumentProcessingService {

    /** 提交新上传的知识库文档；同一文档已经排队时返回 false。 */
    boolean submitKnowledgeDocument(String documentId);

    /** 提交新上传的会话文档；同一文档已经排队时返回 false。 */
    boolean submitSessionDocument(String documentId);

    /** 把已完成或失败的知识库文档重置为待处理后重新提交。 */
    boolean retryKnowledgeDocument(String documentId);

    /** 把已完成或失败的会话文档重置为待处理后重新提交。 */
    boolean retrySessionDocument(String documentId);

    /** 查询当前用户的待处理、处理中、成功和失败文档数量。 */
    DocumentProcessingSummary summary(String userId);
}
