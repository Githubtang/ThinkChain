package com.tyh.chat.rag.consistency;

import java.util.List;

/**
 * 一个文档在 MySQL 切片表与 Supabase 向量表之间的一致性报告。
 *
 * <p>missingChunkIds 表示主库有切片但没有向量；orphanVectorIds 表示向量存在但主库切片已不存在。</p>
 */
public class RagConsistencyReport {

    private String documentId;
    private int chunkCount;
    private int vectorCount;
    private List<String> missingChunkIds = List.of();
    private List<String> orphanVectorIds = List.of();
    private boolean consistent;
    private boolean repaired;

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public int getVectorCount() { return vectorCount; }
    public void setVectorCount(int vectorCount) { this.vectorCount = vectorCount; }
    public List<String> getMissingChunkIds() { return missingChunkIds; }
    public void setMissingChunkIds(List<String> missingChunkIds) { this.missingChunkIds = missingChunkIds; }
    public List<String> getOrphanVectorIds() { return orphanVectorIds; }
    public void setOrphanVectorIds(List<String> orphanVectorIds) { this.orphanVectorIds = orphanVectorIds; }
    public boolean isConsistent() { return consistent; }
    public void setConsistent(boolean consistent) { this.consistent = consistent; }
    public boolean isRepaired() { return repaired; }
    public void setRepaired(boolean repaired) { this.repaired = repaired; }
}
