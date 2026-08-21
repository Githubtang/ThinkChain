package com.tyh.chat.rag.retrieval;

import com.tyh.chat.rag.embedding.client.EmbeddingClient;
import com.tyh.chat.rag.embedding.store.RagEmbeddingMatch;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RAG 统一检索实现。
 *
 * <p>SESSION 资料优先于长期知识库，同一作用域内再按 cosine 相似度降序排列。
 * Supabase 的 HNSW 查询带作用域过滤时可能少于 topK，这是正常情况；本实现不会用低相关切片补足数量。</p>
 */
@Service
public class RagRetrievalServiceImpl implements RagRetrievalService {

    private final EmbeddingClient embeddingClient;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;
    private final RagRetrievalProperties properties;

    public RagRetrievalServiceImpl(EmbeddingClient embeddingClient,
                                   ObjectProvider<RagEmbeddingStore> embeddingStoreProvider,
                                   RagRetrievalProperties properties) {
        this.embeddingClient = embeddingClient;
        this.embeddingStoreProvider = embeddingStoreProvider;
        this.properties = properties;
    }

    @Override
    public List<RagEmbeddingMatch> retrieve(String question,
                                            String conversationId,
                                            List<String> knowledgeBaseIds,
                                            List<String> sessionDocumentIds,
                                            String ragMode,
                                            Integer requestedTopK,
                                            Double requestedMinScore) {
        if (question == null || question.isBlank() || "NONE".equals(normalizeMode(ragMode))) {
            return List.of();
        }
        RagEmbeddingStore store = embeddingStoreProvider.getIfAvailable();
        if (store == null) {
            return List.of();
        }

        int topK = effectiveTopK(requestedTopK);
        double minScore = requestedMinScore != null ? requestedMinScore : properties.getMinScore();
        float[] queryEmbedding = embeddingClient.embed(question.trim());
        String mode = normalizeMode(ragMode);
        List<RagEmbeddingMatch> all = new ArrayList<>();

        if (useSession(mode) && isNonBlank(conversationId)
                && sessionDocumentIds != null && !sessionDocumentIds.isEmpty()) {
            all.addAll(store.searchByScope("SESSION", conversationId.trim(), sessionDocumentIds,
                    queryEmbedding, topK));
        }
        if (useKnowledgeBase(mode) && knowledgeBaseIds != null) {
            for (String knowledgeBaseId : knowledgeBaseIds) {
                if (isNonBlank(knowledgeBaseId)) {
                    all.addAll(store.searchByScope("KB", knowledgeBaseId.trim(), queryEmbedding, topK));
                }
            }
        }

        Map<String, RagEmbeddingMatch> unique = new LinkedHashMap<>();
        for (RagEmbeddingMatch match : all) {
            if (match == null || match.getChunkId() == null || score(match) < minScore) {
                continue;
            }
            RagEmbeddingMatch existing = unique.get(match.getChunkId());
            if (existing == null || score(match) > score(existing)) {
                unique.put(match.getChunkId(), match);
            }
        }

        List<RagEmbeddingMatch> sorted = unique.values().stream()
                .sorted(Comparator
                        .comparing((RagEmbeddingMatch match) -> !"SESSION".equalsIgnoreCase(match.getScopeType()))
                        .thenComparing(Comparator.comparing(RagEmbeddingMatch::getScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))))
                .limit(topK)
                .toList();
        return applyContextBudget(sorted);
    }

    @Override
    public int effectiveTopK(Integer requestedTopK) {
        int value = requestedTopK != null ? requestedTopK : properties.getTopK();
        return Math.max(1, Math.min(value, 50));
    }

    /** 按排序顺序装入上下文，达到字符预算后停止，避免提示词超长。 */
    private List<RagEmbeddingMatch> applyContextBudget(List<RagEmbeddingMatch> matches) {
        int maxChars = Math.max(1, properties.getMaxContextChars());
        int usedChars = 0;
        List<RagEmbeddingMatch> accepted = new ArrayList<>();
        for (RagEmbeddingMatch match : matches) {
            int chars = match.getContent() != null ? match.getContent().length() : 0;
            if (!accepted.isEmpty() && usedChars + chars > maxChars) {
                break;
            }
            if (chars > maxChars) {
                continue;
            }
            accepted.add(match);
            usedChars += chars;
        }
        return List.copyOf(accepted);
    }

    private static String normalizeMode(String mode) {
        return mode == null || mode.isBlank() ? "AUTO" : mode.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean useSession(String mode) {
        return "AUTO".equals(mode) || "SESSION_ONLY".equals(mode) || "SESSION_AND_KB".equals(mode);
    }

    private static boolean useKnowledgeBase(String mode) {
        return "AUTO".equals(mode) || "KB_ONLY".equals(mode) || "SESSION_AND_KB".equals(mode);
    }

    private static double score(RagEmbeddingMatch match) {
        return match.getScore() != null ? match.getScore() : 0.0D;
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
