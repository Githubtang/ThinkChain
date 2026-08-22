package com.tyh.chat.rag.consistency;

/**
 * MySQL 文档切片与 Supabase pgvector 数据的一致性服务。
 *
 * <p>Controller 必须先完成文档归属校验，本接口只负责检查和修复指定 documentId 的数据。</p>
 */
public interface RagConsistencyService {

    /** 只检查，不修改任何数据。 */
    RagConsistencyReport inspect(String documentId);

    /** 删除孤立向量并为缺失向量的切片重新生成 Embedding。 */
    RagConsistencyReport repair(String documentId);
}
