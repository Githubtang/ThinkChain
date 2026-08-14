package com.tyh.chat.rag.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.ChatResponse;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.chat.service.ChatService;
import com.tyh.chat.rag.chat.dto.RagChatRequest;
import com.tyh.chat.rag.chat.dto.RagChatResponse;
import com.tyh.chat.rag.chat.dto.RagSource;
import com.tyh.chat.rag.chat.service.RagChatService;
import com.tyh.chat.rag.embedding.client.EmbeddingClient;
import com.tyh.chat.rag.embedding.store.RagEmbeddingMatch;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import com.tyh.chat.rag.log.domain.RagQueryLog;
import com.tyh.chat.rag.log.service.RagQueryLogService;
import com.tyh.chat.rag.retrieval.RagPromptBuilder;
import com.tyh.chat.log.ChatLogSanitizer;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.core.domain.AjaxResult;
import com.tyh.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 独立 RAG 问答的完整实现。
 *
 * <p>处理链路为：问题向量化 → 按知识库/会话范围检索 → 去重并按相似度排序 →
 * 构造带参考资料的系统提示词 → 复用普通 ChatService 调用模型 → 返回引用来源并记录日志。</p>
 */
@Service
public class RagChatServiceImpl implements RagChatService {

    private static final Logger log = LoggerFactory.getLogger(RagChatServiceImpl.class);
    private static final int DEFAULT_TOP_K = 6;

    private final ChatService chatService;
    private final EmbeddingClient embeddingClient;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;
    private final RagQueryLogService queryLogService;
    private final ObjectMapper objectMapper;
    private final RagPromptBuilder promptBuilder;
    private final ChatLogSanitizer logSanitizer;

    public RagChatServiceImpl(ChatService chatService,
                              EmbeddingClient embeddingClient,
                              ObjectProvider<RagEmbeddingStore> embeddingStoreProvider,
                              RagQueryLogService queryLogService,
                              ObjectMapper objectMapper,
                              RagPromptBuilder promptBuilder,
                              ChatLogSanitizer logSanitizer) {
        this.chatService = chatService;
        this.embeddingClient = embeddingClient;
        this.embeddingStoreProvider = embeddingStoreProvider;
        this.queryLogService = queryLogService;
        this.objectMapper = objectMapper;
        this.promptBuilder = promptBuilder;
        this.logSanitizer = logSanitizer;
    }

    @Override
    public RagChatResponse chat(RagChatRequest request) {
        long start = System.currentTimeMillis();
        // 先创建日志对象，后续无论在哪一步失败，都可以记录尽可能完整的上下文。
        RagQueryLog log = initLog(request);
        try {
            if (request == null) {
                throw new IllegalArgumentException("RAG chat request must not be null");
            }
            if (request.getQuestion() == null || request.getQuestion().isBlank()) {
                throw new IllegalArgumentException("Question must not be blank");
            }
            RagEmbeddingStore store = embeddingStoreProvider.getIfAvailable();
            if (store == null) {
                throw new IllegalStateException("RagEmbeddingStore is unavailable; check vector datasource config");
            }

            int topK = request.getTopK() != null && request.getTopK() > 0 ? request.getTopK() : DEFAULT_TOP_K;
            // 问题和文档切片必须使用兼容的向量模型，才能在同一个语义空间中比较距离。
            float[] queryEmbedding = embeddingClient.embed(request.getQuestion());
            List<RagEmbeddingMatch> matches = retrieve(request, store, queryEmbedding, topK);
            List<RagSource> sources = toSources(matches);

            // 转成普通聊天请求，模型选择、厂商调用和模型日志就不需要重复实现。
            ChatRequest chatRequest = buildChatRequest(request, matches, topK);
            AjaxResult chatResult = chatService.chat(chatRequest, null);
            if (chatResult.isError()) {
                throw new IllegalStateException(String.valueOf(chatResult.get(AjaxResult.MSG_TAG)));
            }

            ChatResponse chatResponse = (ChatResponse) chatResult.get(AjaxResult.DATA_TAG);
            RagChatResponse response = new RagChatResponse();
            response.setAnswer(chatResponse != null ? chatResponse.getContent() : null);
            response.setChatResponse(chatResponse);
            response.setSources(sources);
            response.setElapsedMs(System.currentTimeMillis() - start);

            completeLogQuietly(log, request, response, sources, topK, "SUCCESS", null,
                    System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            completeLogQuietly(log, request, null, List.of(), request != null ? request.getTopK() : null,
                    "FAILED", e.getMessage(), System.currentTimeMillis() - start);
            if (e instanceof ServiceException serviceException) {
                throw serviceException;
            }
            throw new ServiceException("RAG 对话失败，请稍后重试", HttpStatus.ERROR)
                    .setDetailMessage(e.getMessage());
        }
    }

    private void completeLogQuietly(RagQueryLog queryLog, RagChatRequest request, RagChatResponse response,
                                    List<RagSource> sources, Integer topK, String status,
                                    String errorMessage, Long elapsedMs) {
        try {
            completeLog(queryLog, request, response, sources, topK, status, errorMessage, elapsedMs);
        } catch (Exception exception) {
            log.warn("Failed to record RAG query log {}: {}", queryLog.getId(), exception.getMessage());
        }
    }

    private List<RagEmbeddingMatch> retrieve(RagChatRequest request, RagEmbeddingStore store,
                                             float[] queryEmbedding, int topK) {
        // ragMode 决定搜索长期知识库、当前会话文档，或者两者都搜索。
        String mode = normalizedMode(request);
        List<RagEmbeddingMatch> all = new ArrayList<>();
        if (useKnowledgeBase(mode) && request.getKnowledgeBaseIds() != null) {
            for (String knowledgeBaseId : request.getKnowledgeBaseIds()) {
                if (isNonBlank(knowledgeBaseId)) {
                    all.addAll(store.searchByScope("KB", knowledgeBaseId.trim(), queryEmbedding, topK));
                }
            }
        }
        if (useSession(mode) && isNonBlank(request.getConversationId()) && request.getSessionDocumentIds() != null
                && !request.getSessionDocumentIds().isEmpty()) {
            all.addAll(store.searchByScope("SESSION", request.getConversationId(),
                    request.getSessionDocumentIds(), queryEmbedding, topK));
        }
        // 同一切片可能从多个范围命中，只保留分数最高的一条，避免重复内容占用上下文。
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
        return unique.values().stream()
                .sorted(Comparator.comparing(RagChatServiceImpl::score).reversed())
                .limit(topK)
                .toList();
    }

    private ChatRequest buildChatRequest(RagChatRequest request, List<RagEmbeddingMatch> matches, int topK) {
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setConversationId(request.getConversationId());
        chatRequest.setUserId(request.getUserId());
        chatRequest.setModel(request.getModel());
        chatRequest.setOptions(request.getOptions());
        // 本类已经完成检索，关闭普通 ChatService 的再次 RAG 增强，防止重复检索和重复提示词。
        chatRequest.setRagEnabled(false);

        List<Message> messages = new ArrayList<>();
        Message system = new Message();
        system.setRole("system");
        Content systemContent = new Content();
        systemContent.setType("text");
        systemContent.setText(promptBuilder.build(matches, topK));
        system.setContents(List.of(systemContent));
        messages.add(system);

        Message user = new Message();
        user.setRole("user");
        Content userContent = new Content();
        userContent.setType("text");
        userContent.setText(request.getQuestion());
        user.setContents(List.of(userContent));
        messages.add(user);
        chatRequest.setMessages(messages);
        return chatRequest;
    }

    private List<RagSource> toSources(List<RagEmbeddingMatch> matches) {
        List<RagSource> sources = new ArrayList<>();
        for (RagEmbeddingMatch match : matches) {
            RagSource source = new RagSource();
            source.setScopeType(match.getScopeType());
            source.setScopeId(match.getScopeId());
            source.setKnowledgeBaseId(match.getKnowledgeBaseId());
            source.setConversationId(match.getConversationId());
            source.setDocumentId(match.getDocumentId());
            source.setChunkId(match.getChunkId());
            source.setContent(match.getContent());
            source.setScore(match.getScore());
            sources.add(source);
        }
        return sources;
    }

    private RagQueryLog initLog(RagChatRequest request) {
        RagQueryLog log = new RagQueryLog();
        log.setId(UUID.randomUUID().toString());
        if (request != null) {
            log.setConversationId(request.getConversationId());
            log.setUserId(request.getUserId());
            log.setModel(request.getModel());
            log.setQuestion(request.getQuestion());
            log.setRagMode(normalizedMode(request));
            log.setTopK(request.getTopK());
            log.setKnowledgeBaseId(firstId(request.getKnowledgeBaseIds()));
            log.setKnowledgeBaseIds(toJson(request.getKnowledgeBaseIds()));
            log.setSessionDocumentIds(toJson(request.getSessionDocumentIds()));
        }
        log.setEmbeddingModel(embeddingClient.modelName());
        return log;
    }

    private void completeLog(RagQueryLog log, RagChatRequest request, RagChatResponse response,
                             List<RagSource> sources, Integer topK, String status,
                             String errorMessage, Long elapsedMs) {
        log.setTopK(topK);
        log.setHitCount(sources != null ? sources.size() : 0);
        // 命中片段和回答可能很长，保存前统一做脱敏与截断。
        log.setHitChunks(logSanitizer.sanitizeResponse(toJson(sources)));
        log.setElapsedMs(elapsedMs);
        log.setStatus(status);
        log.setErrorMessage(logSanitizer.sanitizeError(errorMessage));
        if (response != null) {
            log.setAnswer(logSanitizer.sanitizeResponse(response.getAnswer()));
        }
        if (sources != null && !sources.isEmpty()) {
            log.setMinScore(sources.stream()
                    .map(RagSource::getScore)
                    .filter(score -> score != null)
                    .min(Double::compareTo)
                    .map(BigDecimal::valueOf)
                    .orElse(null));
        }
        queryLogService.record(log);
    }

    private String normalizedMode(RagChatRequest request) {
        String mode = request.getRagMode();
        if (mode == null || mode.isBlank()) {
            return "KB_ONLY";
        }
        return mode.trim().toUpperCase(Locale.ROOT);
    }

    private boolean useSession(String mode) {
        return "AUTO".equals(mode) || "SESSION_ONLY".equals(mode) || "SESSION_AND_KB".equals(mode);
    }

    private boolean useKnowledgeBase(String mode) {
        return "AUTO".equals(mode) || "KB_ONLY".equals(mode) || "SESSION_AND_KB".equals(mode);
    }

    private String firstId(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.get(0);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private static double score(RagEmbeddingMatch match) {
        return match.getScore() != null ? match.getScore() : 0.0d;
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }
}
