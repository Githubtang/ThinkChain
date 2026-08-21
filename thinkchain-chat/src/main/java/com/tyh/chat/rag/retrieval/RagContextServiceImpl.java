package com.tyh.chat.rag.retrieval;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.rag.embedding.store.RagEmbeddingMatch;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 普通聊天请求的 RAG 增强实现。
 *
 * <p>本类只负责判断普通聊天是否需要 RAG，并把统一检索服务返回的资料放到 system 消息最前面。
 * 实际向量查询由 RagRetrievalService 完成，独立 RAG 接口也复用相同规则。</p>
 */
@Service
public class RagContextServiceImpl implements RagContextService {

    private final RagRetrievalService retrievalService;
    private final RagPromptBuilder promptBuilder;

    public RagContextServiceImpl(RagRetrievalService retrievalService,
                                 RagPromptBuilder promptBuilder) {
        this.retrievalService = retrievalService;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public ChatRequest augment(ChatRequest request) {
        // 未启用且没有选择资料时直接返回，避免不必要的向量模型调用。
        if (!shouldUseRag(request)) {
            return request;
        }
        // 只用最后一条用户文本检索，系统消息和历史模型回答不参与向量查询。
        String question = lastUserText(request);
        if (question.isBlank()) {
            return request;
        }
        int topK = retrievalService.effectiveTopK(request.getRagTopK());
        List<RagEmbeddingMatch> matches = retrievalService.retrieve(
                question,
                request.getConversationId(),
                request.getKnowledgeBaseIds(),
                request.getSessionDocumentIds(),
                request.getRagMode(),
                request.getRagTopK(),
                request.getRagMinScore());
        if (matches.isEmpty()) {
            // 没有命中时不加入空提示词，保持原来的普通聊天行为。
            return request;
        }
        return withRagContext(request, matches, topK);
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
        target.setRagMinScore(source.getRagMinScore());
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

}
