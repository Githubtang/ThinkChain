package com.tyh.chat.rag.document.service;

import com.tyh.chat.rag.document.domain.KnowledgeDocument;

/**
 * 知识库文档解析接口。
 *
 * <p>负责读取上传到服务器的文本文件、拆分切片并保存切片记录。
 * “解析”只产生可检索的文本切片；把切片转换成向量由 RagEmbeddingService 负责。</p>
 */
public interface KnowledgeDocumentParseService {

    /**
     * 解析指定知识库文档并更新文档状态。
     *
     * @param documentId 知识文档 ID
     * @return 更新后的文档对象；解析失败时对象状态为 FAILED，并保存内部错误信息
     */
    KnowledgeDocument parse(String documentId);
}
