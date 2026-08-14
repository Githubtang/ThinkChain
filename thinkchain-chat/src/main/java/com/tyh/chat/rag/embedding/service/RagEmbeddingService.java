package com.tyh.chat.rag.embedding.service;

/** 将已经解析好的文档切片转换为向量并写入向量数据库。 */
public interface RagEmbeddingService {

    /**
     * 向量化一个文档下尚未完成向量化的切片。
     *
     * @param documentId 文档 ID，知识库文档和会话文档都使用该方法
     * @return 本次成功写入向量库的切片数量
     */
    int embedDocument(String documentId);
}
