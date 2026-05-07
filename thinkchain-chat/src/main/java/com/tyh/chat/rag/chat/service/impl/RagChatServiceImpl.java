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
import com.tyh.common.core.domain.AjaxResult;
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

@Service
public class RagChatServiceImpl implements RagChatService {

    private static final int DEFAULT_TOP_K = 6;

    private final ChatService chatService;
    private final EmbeddingClient embeddingClient;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;
    private final RagQueryLogService queryLogService;
    private final ObjectMapper objectMapper;

    public RagChatServiceImpl(ChatService chatService,
                              EmbeddingClient embeddingClient,
                              ObjectProvider<RagEmbeddingStore> embeddingStoreProvider,
                              RagQueryLogService queryLogService,
                              ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.embeddingClient = embeddingClient;
        this.embeddingStoreProvider = embeddingStoreProvider;
        this.queryLogService = queryLogService;
        this.objectMapper = objectMapper;
    }

    @Override
    public RagChatResponse chat(RagChatRequest request) {
        long start = System.currentTimeMillis();
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
            float[] queryEmbedding = embeddingClient.embed(request.getQuestion());
            List<RagEmbeddingMatch> matches = retrieve(request, store, queryEmbedding, topK);
            List<RagSource> sources = toSources(matches);

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

            completeLog(log, request, response, sources, topK, "SUCCESS", null, System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            completeLog(log, request, null, List.of(), request != null ? request.getTopK() : null,
                    "FAILED", e.getMessage(), System.currentTimeMillis() - start);
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private List<RagEmbeddingMatch> retrieve(RagChatRequest request, RagEmbeddingStore store,
                                             float[] queryEmbedding, int topK) {
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
        chatRequest.setRagEnabled(false);

        List<Message> messages = new ArrayList<>();
        Message system = new Message();
        system.setRole("system");
        Content systemContent = new Content();
        systemContent.setType("text");
        systemContent.setText(buildPrompt(matches, topK));
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

    private String buildPrompt(List<RagEmbeddingMatch> matches, int topK) {
        StringBuilder sb = new StringBuilder();
        sb.append("?????????????????????????????????? RAG_CONTEXT ??????????");
        sb.append("???????????????????????????????????????????????????\n\n");
        sb.append("RAG_CONTEXT(topK=").append(topK).append(")\n");
        for (int i = 0; i < matches.size(); i++) {
            RagEmbeddingMatch match = matches.get(i);
            sb.append("[")
                    .append(i + 1)
                    .append("] scope=").append(match.getScopeType())
                    .append(", documentId=").append(match.getDocumentId())
                    .append(", chunkId=").append(match.getChunkId())
                    .append(", score=").append(match.getScore())
                    .append("\n")
                    .append(match.getContent())
                    .append("\n\n");
        }
        return sb.toString();
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
        log.setHitChunks(toJson(sources));
        log.setElapsedMs(elapsedMs);
        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        if (response != null) {
            log.setAnswer(response.getAnswer());
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
