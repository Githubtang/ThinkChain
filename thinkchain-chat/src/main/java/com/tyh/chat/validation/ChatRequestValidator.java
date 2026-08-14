package com.tyh.chat.validation;

import com.tyh.chat.chat.dto.ChatRequest;
import com.tyh.chat.chat.dto.Content;
import com.tyh.chat.chat.dto.Message;
import com.tyh.chat.rag.chat.dto.RagChatRequest;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 对话请求的跨字段业务校验器。
 *
 * <p>DTO 上的 Jakarta 注解适合检查长度、范围和非空；这里检查需要同时观察多个字段的规则，
 * 例如至少有一条 user 消息、SESSION_ONLY 必须同时提供会话和会话文档。</p>
 */
@Component
public class ChatRequestValidator {

    /** 校验普通聊天中消息内容、资源 ID 和会话文档组合关系。 */
    public void validate(ChatRequest request) {
        if (request == null) {
            throw badRequest("聊天请求不能为空");
        }
        boolean hasUserMessage = false;
        if (request.getMessages() != null) {
            for (Message message : request.getMessages()) {
                if (message == null) {
                    throw badRequest("消息列表不能包含空元素");
                }
                if ("user".equalsIgnoreCase(message.getRole())) {
                    hasUserMessage = true;
                }
                validateContents(message.getContents());
            }
        }
        if (!hasUserMessage) {
            throw badRequest("至少需要一条user消息");
        }
        validateIds(request.getKnowledgeBaseIds(), "知识库ID");
        validateIds(request.getSessionDocumentIds(), "会话文档ID");
        if (request.getSessionDocumentIds() != null && !request.getSessionDocumentIds().isEmpty()
                && isBlank(request.getConversationId())) {
            throw badRequest("使用会话文档时必须提供conversationId");
        }
    }

    /** 根据 ragMode 校验独立 RAG 请求是否选择了必需的资料范围。 */
    public void validate(RagChatRequest request) {
        if (request == null) {
            throw badRequest("RAG请求不能为空");
        }
        validateIds(request.getKnowledgeBaseIds(), "知识库ID");
        validateIds(request.getSessionDocumentIds(), "会话文档ID");
        String mode = isBlank(request.getRagMode())
                ? "KB_ONLY"
                : request.getRagMode().trim().toUpperCase(Locale.ROOT);
        if (("KB_ONLY".equals(mode) || "SESSION_AND_KB".equals(mode))
                && isEmpty(request.getKnowledgeBaseIds())) {
            throw badRequest("当前RAG模式必须选择知识库");
        }
        if (("SESSION_ONLY".equals(mode) || "SESSION_AND_KB".equals(mode))
                && (isBlank(request.getConversationId()) || isEmpty(request.getSessionDocumentIds()))) {
            throw badRequest("当前RAG模式必须提供conversationId和会话文档ID");
        }
    }

    private void validateContents(List<Content> contents) {
        if (contents == null) {
            return;
        }
        for (Content content : contents) {
            if (content == null) {
                throw badRequest("消息内容不能包含空元素");
            }
            String type = content.getType() == null ? "" : content.getType().trim().toLowerCase(Locale.ROOT);
            if ("text".equals(type) && isBlank(content.getText())) {
                throw badRequest("text类型内容不能为空");
            }
            if (!"text".equals(type) && isBlank(content.getText()) && isBlank(content.getUrl())) {
                throw badRequest(type + "类型必须提供text或url");
            }
        }
    }

    private void validateIds(List<String> ids, String label) {
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            if (isBlank(id) || id.length() > 64) {
                throw badRequest(label + "不能为空且长度不能超过64个字符");
            }
        }
    }

    private static boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ServiceException badRequest(String message) {
        return new ServiceException(message, HttpStatus.BAD_REQUEST);
    }
}
