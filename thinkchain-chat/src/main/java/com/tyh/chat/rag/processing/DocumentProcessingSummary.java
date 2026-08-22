package com.tyh.chat.rag.processing;

/**
 * 当前用户的文档后台处理概况。
 *
 * <p>这些数量来自主数据库状态，queuedTaskCount 是当前单实例内存队列中尚未完成的任务数。</p>
 */
public class DocumentProcessingSummary {

    private int queuedTaskCount;
    private int knowledgePending;
    private int knowledgeProcessing;
    private int knowledgeReady;
    private int knowledgeFailed;
    private int sessionPending;
    private int sessionProcessing;
    private int sessionReady;
    private int sessionFailed;

    public int getQueuedTaskCount() { return queuedTaskCount; }
    public void setQueuedTaskCount(int queuedTaskCount) { this.queuedTaskCount = queuedTaskCount; }
    public int getKnowledgePending() { return knowledgePending; }
    public void setKnowledgePending(int knowledgePending) { this.knowledgePending = knowledgePending; }
    public int getKnowledgeProcessing() { return knowledgeProcessing; }
    public void setKnowledgeProcessing(int knowledgeProcessing) { this.knowledgeProcessing = knowledgeProcessing; }
    public int getKnowledgeReady() { return knowledgeReady; }
    public void setKnowledgeReady(int knowledgeReady) { this.knowledgeReady = knowledgeReady; }
    public int getKnowledgeFailed() { return knowledgeFailed; }
    public void setKnowledgeFailed(int knowledgeFailed) { this.knowledgeFailed = knowledgeFailed; }
    public int getSessionPending() { return sessionPending; }
    public void setSessionPending(int sessionPending) { this.sessionPending = sessionPending; }
    public int getSessionProcessing() { return sessionProcessing; }
    public void setSessionProcessing(int sessionProcessing) { this.sessionProcessing = sessionProcessing; }
    public int getSessionReady() { return sessionReady; }
    public void setSessionReady(int sessionReady) { this.sessionReady = sessionReady; }
    public int getSessionFailed() { return sessionFailed; }
    public void setSessionFailed(int sessionFailed) { this.sessionFailed = sessionFailed; }
}
