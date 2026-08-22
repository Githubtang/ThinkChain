package com.tyh.chat.security;

import com.tyh.chat.conversation.service.ConversationService;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import com.tyh.chat.rag.knowledge.service.KnowledgeBaseService;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.chat.validation.ChatFilePathResolver;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 聊天资源的级联删除协调服务。
 *
 * <p>一份文档的数据分散在三个位置：Supabase 向量表、主数据库切片/元数据表和服务器磁盘文件。
 * 直接调用某一个 Mapper 只会删掉其中一部分，因此控制器统一通过本服务删除。</p>
 *
 * <p>主数据库操作由 {@link Transactional} 保证事务；Supabase 是独立数据源，不属于同一个分布式事务，
 * 所以先删除向量，向量删除失败就停止后续操作。磁盘文件则等主数据库事务成功提交后再删除。</p>
 */
@Service
public class ChatResourceDeletionService {

    private static final Logger log = LoggerFactory.getLogger(ChatResourceDeletionService.class);

    private final ConversationService conversationService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final SessionDocumentService sessionDocumentService;
    private final KnowledgeChunkService chunkService;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;
    private final ChatFilePathResolver filePathResolver;

    public ChatResourceDeletionService(ConversationService conversationService,
                                       KnowledgeBaseService knowledgeBaseService,
                                       KnowledgeDocumentService knowledgeDocumentService,
                                       SessionDocumentService sessionDocumentService,
                                       KnowledgeChunkService chunkService,
                                       ObjectProvider<RagEmbeddingStore> embeddingStoreProvider,
                                       ChatFilePathResolver filePathResolver) {
        this.conversationService = conversationService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.sessionDocumentService = sessionDocumentService;
        this.chunkService = chunkService;
        this.embeddingStoreProvider = embeddingStoreProvider;
        this.filePathResolver = filePathResolver;
    }

    @Transactional
    public boolean deleteConversation(String conversationId, String userId) {
        // 会话删除前先清理该会话上传的临时文档，再由 ConversationService 删除消息和会话本身。
        SessionDocument query = new SessionDocument();
        query.setConversationId(conversationId);
        query.setUserId(userId);
        for (SessionDocument document : sessionDocumentService.list(query)) {
            deleteSessionDocumentInternal(document);
        }
        return conversationService.deleteConversation(conversationId) > 0;
    }

    @Transactional
    public boolean deleteKnowledgeBase(String knowledgeBaseId, String userId) {
        // 知识库是父资源，必须先逐个清理其文档、切片、向量和文件。
        KnowledgeDocument query = new KnowledgeDocument();
        query.setKnowledgeBaseId(knowledgeBaseId);
        query.setUserId(userId);
        for (KnowledgeDocument document : knowledgeDocumentService.list(query)) {
            deleteKnowledgeDocumentInternal(document);
        }
        return knowledgeBaseService.deleteById(knowledgeBaseId) > 0;
    }

    @Transactional
    public boolean deleteKnowledgeDocument(KnowledgeDocument document) {
        return deleteKnowledgeDocumentInternal(document);
    }

    @Transactional
    public boolean deleteSessionDocument(SessionDocument document) {
        return deleteSessionDocumentInternal(document);
    }

    private boolean deleteKnowledgeDocumentInternal(KnowledgeDocument document) {
        // 删除顺序从外部向量数据到主库子记录，再到文档元数据。
        deleteVectors(document.getId());
        chunkService.deleteByDocumentId(document.getId());
        boolean deleted = knowledgeDocumentService.deleteById(document.getId()) > 0;
        if (deleted) {
            deleteFileAfterCommit(document.getFilePath());
        }
        return deleted;
    }

    private boolean deleteSessionDocumentInternal(SessionDocument document) {
        deleteVectors(document.getId());
        chunkService.deleteByDocumentId(document.getId());
        boolean deleted = sessionDocumentService.deleteById(document.getId()) > 0;
        if (deleted) {
            deleteFileAfterCommit(document.getFilePath());
        }
        return deleted;
    }

    private void deleteVectors(String documentId) {
        RagEmbeddingStore store = embeddingStoreProvider.getIfAvailable();
        if (store == null) {
            // 未配置向量数据源时说明当前环境没有向量存储，继续删除主库数据。
            return;
        }
        try {
            store.deleteByDocumentId(documentId);
        } catch (Exception exception) {
            throw new ServiceException("向量数据删除失败", HttpStatus.ERROR)
                    .setDetailMessage(exception.getMessage());
        }
    }

    private void deleteFileAfterCommit(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        Runnable cleanup = () -> deleteLocalFile(filePath);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 只有数据库真正提交成功才删物理文件；若事务回滚，原文件仍可用于恢复。
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }

    private void deleteLocalFile(String filePath) {
        try {
            Path target = filePathResolver.resolveDeletablePath(filePath);
            Files.deleteIfExists(target);
        } catch (Exception exception) {
            log.warn("Failed to delete uploaded file {}: {}", filePath, exception.getMessage());
        }
    }
}
