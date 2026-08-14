package com.tyh.chat.rag.session.service.impl;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import com.tyh.chat.rag.session.domain.SessionDocument;
import com.tyh.chat.rag.session.service.SessionDocumentParseService;
import com.tyh.chat.rag.session.service.SessionDocumentService;
import com.tyh.common.config.ThinkChainConfig;
import com.tyh.common.utils.file.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 会话临时文本文件解析实现。
 *
 * <p>处理方式与知识库文档相同，但生成的切片标记为 SESSION，并绑定 conversationId，
 * 检索时只能在对应会话和用户选中的文档范围内使用。</p>
 */
@Service
public class SessionDocumentParseServiceImpl implements SessionDocumentParseService {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 120;
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "csv", "json", "xml", "html", "htm",
            "log", "sql", "java", "js", "ts", "css", "yml", "yaml", "properties");

    private final SessionDocumentService documentService;
    private final KnowledgeChunkService chunkService;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;

    public SessionDocumentParseServiceImpl(SessionDocumentService documentService,
                                           KnowledgeChunkService chunkService,
                                           ObjectProvider<RagEmbeddingStore> embeddingStoreProvider) {
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.embeddingStoreProvider = embeddingStoreProvider;
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
            List<String> chunks = splitText(text);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Document text is empty");
            }
            // 重新解析前删除旧向量和旧切片，保证一个文档只有当前版本的数据。
            embeddingStoreProvider.ifAvailable(store -> store.deleteByDocumentId(document.getId()));
            chunkService.deleteByDocumentId(document.getId());
            int tokenCount = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String content = chunks.get(i);
                // Token 数用于粗略衡量上下文大小，目前采用字符数除以 4 的近似算法。
                int chunkTokens = estimateTokens(content);
                tokenCount += chunkTokens;
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setScopeType("SESSION");
                chunk.setScopeId(document.getConversationId());
                chunk.setConversationId(document.getConversationId());
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(content);
                chunk.setContentHash(sha256(content));
                chunk.setTokenCount(chunkTokens);
                chunk.setCharCount(content.length());
                chunk.setSectionTitle(document.getOriginalFileName());
                chunk.setEmbeddingStatus("PENDING");
                chunkService.create(chunk);
            }
            mark(document, "READY", chunks.size(), tokenCount, null);
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
        String extension = FilenameUtils.getExtension(sourceName);
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!TEXT_EXTENSIONS.contains(extension)) {
            throw new UnsupportedOperationException("Unsupported document type: " + extension);
        }
        Path path = resolveLocalPath(document.getFilePath());
        return Files.readString(path, StandardCharsets.UTF_8);
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

    private List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        String normalized = text == null ? "" : text.replace("\r\n", "\n").trim();
        if (normalized.isBlank()) {
            return chunks;
        }
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            chunks.add(normalized.substring(start, end).trim());
            if (end == normalized.length()) {
                break;
            }
            // 保留 120 字符重叠，减少上下文在切片边界处断裂。
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
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

    private static int estimateTokens(String text) {
        return Math.max(1, (int) Math.ceil((text == null ? 0 : text.length()) / 4.0));
    }

    private static String sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }
}
