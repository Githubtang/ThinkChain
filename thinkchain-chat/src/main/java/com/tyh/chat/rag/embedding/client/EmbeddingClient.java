package com.tyh.chat.rag.embedding.client;

/**
 * 文本向量模型的统一接口。
 *
 * <p>向量是一组浮点数，用来表示文本语义。语义越接近的文本，其向量距离通常越近。
 * 上层 RAG 代码只依赖此接口，因此以后替换向量模型时不需要修改检索流程。</p>
 */
public interface EmbeddingClient {

    /** 返回当前向量模型名称，用于记录向量由哪个模型生成。 */
    String modelName();

    /**
     * 把一段文本转换为固定维度的浮点数组。
     *
     * @param text 需要向量化的文本
     * @return 向量数组；数组维度必须与 PostgreSQL vector 字段定义一致
     */
    float[] embed(String text);
}
