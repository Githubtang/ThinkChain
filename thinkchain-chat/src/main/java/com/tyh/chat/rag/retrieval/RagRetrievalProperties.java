package com.tyh.chat.rag.retrieval;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAG 检索的统一配置。
 *
 * <p>普通聊天和独立 RAG 接口都读取这里的默认值，避免两个入口分别维护 topK、
 * 相似度阈值和上下文长度。请求中显式传值时优先使用请求值。</p>
 */
@Component
@ConfigurationProperties(prefix = "thinkchain.rag.retrieval")
public class RagRetrievalProperties {

    /** 默认召回切片数量。 */
    private int topK = 6;

    /** cosine 相似度最低值；低于该值的切片不交给聊天模型。 */
    private double minScore = 0.35D;

    /** 所有召回切片允许占用的最大字符数，防止提示词无限增长。 */
    private int maxContextChars = 12000;

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }

    public void setMaxContextChars(int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }
}
