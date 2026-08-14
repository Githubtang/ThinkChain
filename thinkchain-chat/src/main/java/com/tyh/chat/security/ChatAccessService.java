package com.tyh.chat.security;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.conversation.domain.ChatConversation;
import com.tyh.chat.conversation.service.ConversationService;
import com.tyh.chat.rag.chat.dto.RagChatRequest;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.knowledge.domain.KnowledgeBase;
import com.tyh.chat.rag.knowledge.service.KnowledgeBaseService;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.exception.ServiceException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * thinkchain-chat 业务资源的统一归属校验服务。
 *
 * <p>它解决“用户 A 猜到用户 B 的 ID 后访问其会话或知识库”的越权问题。
 * 控制器在查询、更新、删除或使用 RAG 资料前调用这里，服务会查询资源并比较 owner userId。</p>
 *
 * <p>请求中的 userId 永远不能作为身份依据；prepare 方法会使用 JWT 中的当前用户覆盖客户端值。</p>
 */
@Service
public class ChatAccessService {

    private final CurrentUserProvider currentUserProvider;
    private final ConversationService conversationService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final SessionDocumentService sessionDocumentService;

    public ChatAccessService(CurrentUserProvider currentUserProvider,
                             ConversationService conversationService,
                             KnowledgeBaseService knowledgeBaseService,
                             KnowledgeDocumentService knowledgeDocumentService,
                             SessionDocumentService sessionDocumentService) {
        this.currentUserProvider = currentUserProvider;
        this.conversationService = conversationService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.sessionDocumentService = sessionDocumentService;
    }

    public String currentUserId() {
        return currentUserProvider.currentUserId();
    }

    public void prepare(ChatRequest request) {
        // 先绑定可信登录身份，再验证请求引用的每个资源都属于该用户。
        String userId = currentUserId();
        request.setUserId(userId);
        if (hasText(request.getConversationId())) {
            requireConversation(request.getConversationId());
        }
        requireKnowledgeBases(request.getKnowledgeBaseIds());
        requireSessionDocuments(request.getSessionDocumentIds(), request.getConversationId());
    }

    public void prepare(RagChatRequest request) {
        // RAG 专用 DTO 与普通聊天执行相同的身份绑定和资料归属检查。
        String userId = currentUserId();
        request.setUserId(userId);
        if (hasText(request.getConversationId())) {
            requireConversation(request.getConversationId());
        }
        requireKnowledgeBases(request.getKnowledgeBaseIds());
        requireSessionDocuments(request.getSessionDocumentIds(), request.getConversationId());
    }

    public ChatConversation requireConversation(String id) {
        // requireXxx 方法既返回已查询对象，也在不存在或越权时立即终止请求。
        ChatConversation conversation = conversationService.getConversation(id);
        requireOwned(conversation != null ? conversation.getUserId() : null, "会话", id);
        return conversation;
    }

    public KnowledgeBase requireKnowledgeBase(String id) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getById(id);
        requireOwned(knowledgeBase != null ? knowledgeBase.getUserId() : null, "知识库", id);
        return knowledgeBase;
    }

    public KnowledgeDocument requireKnowledgeDocument(String id) {
        KnowledgeDocument document = knowledgeDocumentService.getById(id);
        requireOwned(document != null ? document.getUserId() : null, "知识文档", id);
        return document;
    }

    public SessionDocument requireSessionDocument(String id) {
        SessionDocument document = sessionDocumentService.getById(id);
        requireOwned(document != null ? document.getUserId() : null, "会话文档", id);
        return document;
    }

    private void requireKnowledgeBases(List<String> ids) {
        if (ids != null) {
            ids.forEach(this::requireKnowledgeBase);
        }
    }

    private void requireSessionDocuments(List<String> ids, String conversationId) {
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            SessionDocument document = requireSessionDocument(id);
            // 文档属于当前用户还不够，它还必须属于本次指定的会话。
            if (hasText(conversationId) && !Objects.equals(conversationId, document.getConversationId())) {
                throw new ServiceException("会话文档不属于当前会话", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void requireOwned(String ownerId, String resourceName, String resourceId) {
        // 不存在返回 404，存在但属于其他用户返回 403，便于区分数据缺失和权限不足。
        if (ownerId == null) {
            throw new ServiceException(resourceName + "不存在: " + resourceId, HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(currentUserId(), ownerId)) {
            throw new ServiceException("无权访问该" + resourceName, HttpStatus.FORBIDDEN);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
