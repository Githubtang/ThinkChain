package com.tyh.chat.rag.session.service.impl;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.extractor.DocumentTextExtractor;
import com.tyh.chat.rag.document.service.DocumentChunker;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentParseService;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.common.config.ThinkChainConfig;
import com.tyh.common.utils.file.FileUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * 会话临时文档解析和切片实现。
 *
 * <p>DocumentTextExtractor 负责读取文本、PDF 和 Office 文件；本类负责生成 SESSION 切片并绑定
 * conversationId，检索时只能在对应会话和用户选中的文档范围内使用。</p>
 */
@Service
public class SessionDocumentParseServiceImpl implements SessionDocumentParseService {

    private final SessionDocumentService documentService;
    private final KnowledgeChunkService chunkService;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;
    private final DocumentTextExtractor textExtractor;
    private final DocumentChunker chunker;

    public SessionDocumentParseServiceImpl(SessionDocumentService documentService,
                                           KnowledgeChunkService chunkService,
                                           ObjectProvider<RagEmbeddingStore> embeddingStoreProvider,
                                           DocumentTextExtractor textExtractor,
                                           DocumentChunker chunker) {
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.embeddingStoreProvider = embeddingStoreProvider;
        this.textExtractor = textExtractor;
        this.chunker = chunker;
    }

    @Override
    public SessionDocument parse(String documentId) {
        // 元数据提供磁盘路径、原始文件名和所属会话。
        SessionDocument document = documentService.getById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Session document not found: " + documentId);
        }
        try {
            mark(document, "PARSING", null, null, null);
            String text = readText(document);
            List<DocumentChunker.Chunk> chunks = chunker.split(text, document.getOriginalFileName());
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Document text is empty");
            }
            // 重新解析前删除旧向量和旧切片，保证一个文档只有当前版本的数据。
            embeddingStoreProvider.ifAvailable(store -> store.deleteByDocumentId(document.getId()));
            chunkService.deleteByDocumentId(document.getId());
            int tokenCount = 0;
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunker.Chunk parsedChunk = chunks.get(i);
                String content = parsedChunk.content();
                // Token 数用于粗略衡量上下文大小，目前采用字符数除以 4 的近似算法。
                int chunkTokens = chunker.estimateTokens(content);
                tokenCount += chunkTokens;
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setScopeType("SESSION");
                chunk.setScopeId(document.getConversationId());
                chunk.setConversationId(document.getConversationId());
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(content);
                chunk.setContentHash(chunker.sha256(content));
                chunk.setTokenCount(chunkTokens);
                chunk.setCharCount(content.length());
                chunk.setPageNumber(parsedChunk.pageNumber());
                chunk.setSectionTitle(parsedChunk.sectionTitle());
                chunk.setEmbeddingStatus("PENDING");
                chunkService.create(chunk);
            }
            // 此时只有文本切片可用，向量尚未完成，不能提前显示 READY。
            mark(document, "EMBEDDING", chunks.size(), tokenCount, null);
        } catch (Exception e) {
            // 失败状态和内部原因落库，控制器只向客户端返回通用解析失败信息。
            mark(document, "FAILED", 0, 0, e.getMessage());
        }
        return documentService.getById(documentId);
    }

    private String readText(SessionDocument document) throws Exception {
        String sourceName = document.getOriginalFileName() != null
                ? document.getOriginalFileName()
                : document.getFilePath();
        Path path = resolveLocalPath(document.getFilePath());
        return textExtractor.extract(path, sourceName);
    }

    private Path resolveLocalPath(String filePath) {
        // 将 /profile/upload/... 访问路径还原为 ThinkChainConfig.profile 下的本地文件路径。
        String relativePath = FileUtils.stripPrefix(filePath);
        if (relativePath == null || relativePath.isBlank()) {
            relativePath = filePath;
        }
        while (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            relativePath = relativePath.substring(1);
        }
        return Path.of(ThinkChainConfig.getProfile(), relativePath);
    }

    private void mark(SessionDocument document, String status, Integer chunkCount, Integer tokenCount, String errorMessage) {
        document.setParseStatus(status);
        if (chunkCount != null) {
            document.setChunkCount(chunkCount);
        }
        if (tokenCount != null) {
            document.setTokenCount(tokenCount);
        }
        document.setErrorMessage(errorMessage);
        documentService.update(document);
    }

}
