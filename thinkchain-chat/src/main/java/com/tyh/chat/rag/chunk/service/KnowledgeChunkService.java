package com.tyh.chat.rag.chunk.service;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;

import java.util.List;

/**
 * 文档切片数据服务。
 *
 * <p>大文档不会整体发送给模型，而是先拆成长度适中的“切片”。
 * 每个切片都能单独生成向量并参与相似度检索。</p>
 */
public interface KnowledgeChunkService {

    /** 根据切片 ID 查询单条切片。 */
    KnowledgeChunk getById(String id);

    /** 根据文档、作用域等非空字段筛选切片。 */
    List<KnowledgeChunk> list(KnowledgeChunk query);

    /** 新增切片；实现类会补充 ID、默认状态和创建时间。 */
    int create(KnowledgeChunk chunk);

    /** 更新已有切片。 */
    int update(KnowledgeChunk chunk);

    /** 删除一个文档产生的全部切片，返回删除数量。 */
    int deleteByDocumentId(String documentId);
}
