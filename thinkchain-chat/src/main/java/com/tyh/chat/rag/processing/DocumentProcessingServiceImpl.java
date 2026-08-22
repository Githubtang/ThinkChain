package com.tyh.chat.rag.processing;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentParseService;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.embedding.service.RagEmbeddingService;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentParseService;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.chat.log.ChatLogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * 文档后台处理实现。
 *
 * <p>每个任务执行“状态抢占 → 解析切片 → 向量化 → READY/FAILED”。数据库条件更新负责跨线程幂等，
 * 内存集合负责避免同一实例重复排队。应用重新启动时会把上次中断的处理中状态恢复为 PENDING。</p>
 */
@Service
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingServiceImpl.class);
    private final Executor executor;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KnowledgeDocumentParseService knowledgeParseService;
    private final SessionDocumentService sessionDocumentService;
    private final SessionDocumentParseService sessionParseService;
    private final RagEmbeddingService embeddingService;
    private final KnowledgeChunkService chunkService;
    private final ChatLogSanitizer logSanitizer;
    private final Set<String> queuedTasks = ConcurrentHashMap.newKeySet();

    public DocumentProcessingServiceImpl(@Qualifier("documentTaskExecutor") Executor executor,
                                         KnowledgeDocumentService knowledgeDocumentService,
                                         KnowledgeDocumentParseService knowledgeParseService,
                                         SessionDocumentService sessionDocumentService,
                                         SessionDocumentParseService sessionParseService,
                                         RagEmbeddingService embeddingService,
                                         KnowledgeChunkService chunkService,
                                         ChatLogSanitizer logSanitizer) {
        this.executor = executor;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.knowledgeParseService = knowledgeParseService;
        this.sessionDocumentService = sessionDocumentService;
        this.sessionParseService = sessionParseService;
        this.embeddingService = embeddingService;
        this.chunkService = chunkService;
        this.logSanitizer = logSanitizer;
    }

    @Override
    public boolean submitKnowledgeDocument(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return false;
        }
        return enqueue("KB:" + documentId, () -> processKnowledgeDocument(documentId));
    }

    @Override
    public boolean submitSessionDocument(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return false;
        }
        return enqueue("SESSION:" + documentId, () -> processSessionDocument(documentId));
    }

    @Override
    public boolean retryKnowledgeDocument(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return false;
        }
        knowledgeDocumentService.requestProcessing(documentId);
        return submitKnowledgeDocument(documentId);
    }

    @Override
    public boolean retrySessionDocument(String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return false;
        }
        sessionDocumentService.requestProcessing(documentId);
        return submitSessionDocument(documentId);
    }

    @Override
    public DocumentProcessingSummary summary(String userId) {
        DocumentProcessingSummary summary = new DocumentProcessingSummary();
        summary.setQueuedTaskCount(queuedCountForUser(userId));
        summary.setKnowledgePending(countKnowledge(userId, "UPLOADED") + countKnowledge(userId, "PENDING"));
        summary.setKnowledgeProcessing(countKnowledge(userId, "PARSING")
                + countKnowledge(userId, "CHUNKING") + countKnowledge(userId, "EMBEDDING"));
        summary.setKnowledgeReady(countKnowledge(userId, "READY"));
        summary.setKnowledgeFailed(countKnowledge(userId, "FAILED"));
        summary.setSessionPending(countSession(userId, "PENDING"));
        summary.setSessionProcessing(countSession(userId, "PARSING")
                + countSession(userId, "CHUNKING") + countSession(userId, "EMBEDDING"));
        summary.setSessionReady(countSession(userId, "READY"));
        summary.setSessionFailed(countSession(userId, "FAILED"));
        return summary;
    }

    /** 单机启动恢复：此时本实例没有仍在运行的旧任务，因此可安全恢复中断状态。 */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedTasks() {
        int recovered = knowledgeDocumentService.resetInterruptedProcessing()
                + sessionDocumentService.resetInterruptedProcessing();
        if (recovered > 0) {
            log.info("Recovered {} interrupted document processing tasks", recovered);
        }
        submitPendingTasks();
    }

    /** 定期补提因队列瞬时满、进程关闭等原因尚未入队的 PENDING/UPLOADED 文档。 */
    @Scheduled(fixedDelayString = "${thinkchain.rag.processing.pollDelayMs:60000}")
    public void submitPendingTasks() {
        submitKnowledgeDocumentsWithStatus("UPLOADED");
        submitKnowledgeDocumentsWithStatus("PENDING");
        SessionDocument sessionQuery = new SessionDocument();
        sessionQuery.setParseStatus("PENDING");
        for (SessionDocument document : sessionDocumentService.list(sessionQuery)) {
            submitSessionDocument(document.getId());
        }
    }

    private void submitKnowledgeDocumentsWithStatus(String status) {
        KnowledgeDocument query = new KnowledgeDocument();
        query.setStatus(status);
        for (KnowledgeDocument document : knowledgeDocumentService.list(query)) {
            submitKnowledgeDocument(document.getId());
        }
    }

    private boolean enqueue(String taskKey, Runnable task) {
        if (!queuedTasks.add(taskKey)) {
            return false;
        }
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } finally {
                    queuedTasks.remove(taskKey);
                }
            });
            return true;
        } catch (RuntimeException exception) {
            queuedTasks.remove(taskKey);
            log.warn("Document task queue rejected {}: {}", taskKey, exception.getMessage());
            return false;
        }
    }

    private void processKnowledgeDocument(String documentId) {
        if (knowledgeDocumentService.claimProcessing(documentId) == 0) {
            return;
        }
        try {
            KnowledgeDocument parsed = knowledgeParseService.parse(documentId);
            if (parsed == null) {
                return;
            }
            if ("FAILED".equals(parsed.getStatus())) {
                markKnowledge(parsed, "FAILED", parsed.getErrorMessage());
                return;
            }
            markKnowledge(parsed, "EMBEDDING", null);
            embeddingService.embedDocument(documentId);
            boolean remaining = hasUnembeddedChunks(documentId);
            markKnowledge(parsed, remaining ? "FAILED" : "READY",
                    remaining ? "部分切片向量化失败，可重新提交处理任务" : null);
        } catch (Exception exception) {
            KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
            if (document != null) {
                markKnowledge(document, "FAILED", safeError(exception));
            }
            log.warn("Knowledge document processing failed, documentId={}: {}", documentId, exception.getMessage());
        }
    }

    private void processSessionDocument(String documentId) {
        if (sessionDocumentService.claimProcessing(documentId) == 0) {
            return;
        }
        try {
            SessionDocument parsed = sessionParseService.parse(documentId);
            if (parsed == null) {
                return;
            }
            if ("FAILED".equals(parsed.getParseStatus())) {
                markSession(parsed, "FAILED", parsed.getErrorMessage());
                return;
            }
            markSession(parsed, "EMBEDDING", null);
            embeddingService.embedDocument(documentId);
            boolean remaining = hasUnembeddedChunks(documentId);
            markSession(parsed, remaining ? "FAILED" : "READY",
                    remaining ? "部分切片向量化失败，可重新提交处理任务" : null);
        } catch (Exception exception) {
            SessionDocument document = sessionDocumentService.getById(documentId);
            if (document != null) {
                markSession(document, "FAILED", safeError(exception));
            }
            log.warn("Session document processing failed, documentId={}: {}", documentId, exception.getMessage());
        }
    }

    private boolean hasUnembeddedChunks(String documentId) {
        return !listChunks(documentId, "PENDING").isEmpty() || !listChunks(documentId, "FAILED").isEmpty();
    }

    private List<KnowledgeChunk> listChunks(String documentId, String status) {
        KnowledgeChunk query = new KnowledgeChunk();
        query.setDocumentId(documentId);
        query.setEmbeddingStatus(status);
        return chunkService.list(query);
    }

    private void markKnowledge(KnowledgeDocument document, String status, String errorMessage) {
        document.setStatus(status);
        document.setErrorMessage(errorMessage);
        if ("READY".equals(status) || "FAILED".equals(status)) {
            document.setProcessingFinishedAt(new Date());
        }
        knowledgeDocumentService.update(document);
    }

    private void markSession(SessionDocument document, String status, String errorMessage) {
        document.setParseStatus(status);
        document.setErrorMessage(errorMessage);
        if ("READY".equals(status) || "FAILED".equals(status)) {
            document.setProcessingFinishedAt(new Date());
        }
        sessionDocumentService.update(document);
    }

    private int countKnowledge(String userId, String status) {
        KnowledgeDocument query = new KnowledgeDocument();
        query.setUserId(userId);
        query.setStatus(status);
        return knowledgeDocumentService.list(query).size();
    }

    private int countSession(String userId, String status) {
        SessionDocument query = new SessionDocument();
        query.setUserId(userId);
        query.setParseStatus(status);
        return sessionDocumentService.list(query).size();
    }

    /** queuedTasks 是单实例内存集合；查询时再校验文档 owner，避免向普通用户暴露全局任务量。 */
    private int queuedCountForUser(String userId) {
        int count = 0;
        for (String taskKey : queuedTasks) {
            if (taskKey.startsWith("KB:")) {
                KnowledgeDocument document = knowledgeDocumentService.getById(taskKey.substring(3));
                if (document != null && userId.equals(document.getUserId())) {
                    count++;
                }
            } else if (taskKey.startsWith("SESSION:")) {
                SessionDocument document = sessionDocumentService.getById(taskKey.substring(8));
                if (document != null && userId.equals(document.getUserId())) {
                    count++;
                }
            }
        }
        return count;
    }

    /** 错误落库前先隐藏密钥并限制长度，异常没有 message 时至少保留类型名。 */
    private String safeError(Exception exception) {
        String message = exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getSimpleName();
        return logSanitizer.sanitizeError(message);
    }

}
