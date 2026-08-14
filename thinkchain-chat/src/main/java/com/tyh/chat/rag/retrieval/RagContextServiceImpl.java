package com.tyh.chat.rag.retrieval;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
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
 * 普通聊天请求的 RAG 增强实现。
 *
 * <p>与 RagChatServiceImpl 的区别是：本类只负责给 ChatRequest 增加参考资料，
 * 不直接调用模型、不生成独立 RAG 响应，也不写 RAG 查询日志。</p>
 */
@Service
public class RagContextServiceImpl implements RagContextService {

    private static final int DEFAULT_TOP_K = 6;

    private final EmbeddingClient embeddingClient;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;
    private final RagPromptBuilder promptBuilder;

    public RagContextServiceImpl(EmbeddingClient embeddingClient,
                                 ObjectProvider<RagEmbeddingStore> embeddingStoreProvider,
                                 RagPromptBuilder promptBuilder) {
        this.embeddingClient = embeddingClient;
        this.embeddingStoreProvider = embeddingStoreProvider;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public ChatRequest augment(ChatRequest request) {
        // 未启用且没有选择资料时直接返回，避免不必要的向量模型调用。
        if (!shouldUseRag(request)) {
            return request;
        }
        RagEmbeddingStore embeddingStore = embeddingStoreProvider.getIfAvailable();
        if (embeddingStore == null) {
            return request;
        }
        // 只用最后一条用户文本检索，系统消息和历史模型回答不参与向量查询。
        String question = lastUserText(request);
        if (question.isBlank()) {
            return request;
        }
        int topK = request.getRagTopK() != null && request.getRagTopK() > 0 ? request.getRagTopK() : DEFAULT_TOP_K;
        float[] queryEmbedding = embeddingClient.embed(question);
        List<RagEmbeddingMatch> matches = retrieve(request, embeddingStore, queryEmbedding, topK);
        if (matches.isEmpty()) {
            // 没有命中时不加入空提示词，保持原来的普通聊天行为。
            return request;
        }
        return withRagContext(request, matches, topK);
    }

    private List<RagEmbeddingMatch> retrieve(ChatRequest request, RagEmbeddingStore embeddingStore,
                                             float[] queryEmbedding, int topK) {
        String mode = normalizedMode(request);
        List<RagEmbeddingMatch> all = new ArrayList<>();
        if (useSession(mode) && isNonBlank(request.getConversationId())) {
            List<String> documentIds = request.getSessionDocumentIds() != null
                    ? request.getSessionDocumentIds()
                    : List.of();
            if (!documentIds.isEmpty()) {
                all.addAll(embeddingStore.searchByScope("SESSION", request.getConversationId(), documentIds, queryEmbedding, topK));
            }
        }
        if (useKnowledgeBase(mode) && request.getKnowledgeBaseIds() != null) {
            for (String knowledgeBaseId : request.getKnowledgeBaseIds()) {
                if (isNonBlank(knowledgeBaseId)) {
                    all.addAll(embeddingStore.searchByScope("KB", knowledgeBaseId.trim(), queryEmbedding, topK));
                }
            }
        }
        // 同一个 chunkId 去重，只保留相似度最高的命中记录。
        Map<String, RagEmbeddingMatch> unique = new LinkedHashMap<>();
        for (RagEmbeddingMatch match : all) {
            if (match.getChunkId() == null) {
                continue;
            }
            RagEmbeddingMatch existing = unique.get(match.getChunkId());
            if (existing == null || score(match) > score(existing)) {
                unique.put(match.getChunkId(), match);
            }
        }
        // SESSION 资料优先于 KB 资料；同一作用域内再按相似度从高到低排序。
        return unique.values().stream()
                .sorted(Comparator
                        .comparing((RagEmbeddingMatch match) -> !"SESSION".equalsIgnoreCase(match.getScopeType()))
                        .thenComparing(Comparator.comparing(RagEmbeddingMatch::getScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))))
                .limit(topK)
                .toList();
    }

    private ChatRequest withRagContext(ChatRequest source, List<RagEmbeddingMatch> matches, int topK) {
        // 创建副本而不是直接修改原请求，避免日志或其他调用方看到被悄悄改变的数据。
        ChatRequest target = copyRequest(source);
        List<Message> messages = new ArrayList<>();
        Message system = new Message();
        system.setRole("system");
        Content content = new Content();
        content.setType("text");
        content.setText(promptBuilder.build(matches, topK));
        system.setContents(List.of(content));
        messages.add(system);
        if (source.getMessages() != null) {
            messages.addAll(source.getMessages());
        }
        target.setMessages(messages);
        return target;
    }

    private ChatRequest copyRequest(ChatRequest source) {
        ChatRequest target = new ChatRequest();
        target.setConversationId(source.getConversationId());
        target.setUserId(source.getUserId());
        target.setModel(source.getModel());
        target.setSystemPrompt(source.getSystemPrompt());
        target.setMessages(source.getMessages());
        target.setOptions(source.getOptions());
        target.setStream(source.getStream());
        target.setRagEnabled(source.getRagEnabled());
        target.setKnowledgeBaseIds(source.getKnowledgeBaseIds());
        target.setSessionDocumentIds(source.getSessionDocumentIds());
        target.setRagTopK(source.getRagTopK());
        target.setRagMode(source.getRagMode());
        return target;
    }

    private boolean shouldUseRag(ChatRequest request) {
        if (request == null) {
            return false;
        }
        if (Boolean.FALSE.equals(request.getRagEnabled())) {
            return false;
        }
        String mode = normalizedMode(request);
        if ("NONE".equals(mode)) {
            return false;
        }
        if (Boolean.TRUE.equals(request.getRagEnabled())) {
            return true;
        }
        return (request.getSessionDocumentIds() != null && !request.getSessionDocumentIds().isEmpty())
                || (request.getKnowledgeBaseIds() != null && !request.getKnowledgeBaseIds().isEmpty());
    }

    private boolean useSession(String mode) {
        return "AUTO".equals(mode) || "SESSION_ONLY".equals(mode) || "SESSION_AND_KB".equals(mode);
    }

    private boolean useKnowledgeBase(String mode) {
        return "AUTO".equals(mode) || "KB_ONLY".equals(mode) || "SESSION_AND_KB".equals(mode);
    }

    private String normalizedMode(ChatRequest request) {
        String mode = request.getRagMode();
        if (mode == null || mode.isBlank()) {
            return "AUTO";
        }
        return mode.trim().toUpperCase(Locale.ROOT);
    }

    private String lastUserText(ChatRequest request) {
        if (request.getMessages() == null) {
            return "";
        }
        for (int i = request.getMessages().size() - 1; i >= 0; i--) {
            Message message = request.getMessages().get(i);
            if (message == null || message.getRole() == null || !"user".equalsIgnoreCase(message.getRole())) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            if (message.getContents() != null) {
                for (Content content : message.getContents()) {
                    if (content != null && "text".equalsIgnoreCase(content.getType()) && content.getText() != null) {
                        sb.append(content.getText());
                    }
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static double score(RagEmbeddingMatch match) {
        return match.getScore() != null ? match.getScore() : 0.0d;
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
