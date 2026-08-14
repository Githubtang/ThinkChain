package com.tyh.chat.rag.document.service.impl;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.service.KnowledgeDocumentParseService;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
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
 * 知识库文本文件解析实现。
 *
 * <p>当前阶段只支持可直接按 UTF-8 读取的文本类文件，不支持 PDF、Word。
 * 文本按 1200 个字符切片，相邻切片重叠 120 个字符，重叠可以减少一句话刚好在边界被截断造成的信息丢失。</p>
 */
@Service
public class KnowledgeDocumentParseServiceImpl implements KnowledgeDocumentParseService {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 120;
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "csv", "json", "xml", "html", "htm",
            "log", "sql", "java", "js", "ts", "css", "yml", "yaml", "properties");

    private final KnowledgeDocumentService documentService;
    private final KnowledgeChunkService chunkService;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;

    public KnowledgeDocumentParseServiceImpl(KnowledgeDocumentService documentService,
                                             KnowledgeChunkService chunkService,
                                             ObjectProvider<RagEmbeddingStore> embeddingStoreProvider) {
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.embeddingStoreProvider = embeddingStoreProvider;
    }

    @Override
    public KnowledgeDocument parse(String documentId) {
        // 先查元数据，因为磁盘路径、知识库归属和展示标题都来自该记录。
        KnowledgeDocument document = documentService.getById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("Knowledge document not found: " + documentId);
        }
        try {
            // 状态会被前端用于显示当前处理阶段：读取中、切片中、完成或失败。
            mark(document, "PARSING", null, null);
            String text = readText(document);
            mark(document, "CHUNKING", null, null);
            List<String> chunks = splitText(text);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Document text is empty");
            }
            // 允许重新解析：先清理旧向量和旧切片，避免同一文档保留两套重复数据。
            embeddingStoreProvider.ifAvailable(store -> store.deleteByDocumentId(document.getId()));
            chunkService.deleteByDocumentId(document.getId());
            for (int i = 0; i < chunks.size(); i++) {
                String content = chunks.get(i);
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setScopeType("KB");
                chunk.setScopeId(document.getKnowledgeBaseId());
                chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(content);
                // 内容哈希可用于判断内容是否变化；Token 数目前只是按字符数估算，不是模型精确计数。
                chunk.setContentHash(sha256(content));
                chunk.setTokenCount(estimateTokens(content));
                chunk.setCharCount(content.length());
                chunk.setSectionTitle(document.getTitle() != null ? document.getTitle() : document.getFileName());
                chunk.setEmbeddingStatus("PENDING");
                chunkService.create(chunk);
            }
            mark(document, "COMPLETED", chunks.size(), null);
        } catch (Exception e) {
            // 解析失败不把异常细节直接返回前端，而是保存到文档记录供服务端排查。
            mark(document, "FAILED", 0, e.getMessage());
        }
        return documentService.getById(documentId);
    }

    private String readText(KnowledgeDocument document) throws Exception {
        String sourceName = document.getFileName() != null ? document.getFileName() : document.getFilePath();
        String extension = FilenameUtils.getExtension(sourceName);
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (!TEXT_EXTENSIONS.contains(extension)) {
            throw new UnsupportedOperationException("Unsupported document type: " + extension);
        }
        Path path = resolveLocalPath(document.getFilePath());
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private Path resolveLocalPath(String filePath) {
        // 数据库存储的是 /profile/upload/... 形式的访问路径，这里转换回服务器磁盘路径。
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
            // 下一片向前回退一段形成重叠，同时至少前进 1 个字符，避免死循环。
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }

    private void mark(KnowledgeDocument document, String status, Integer chunkCount, String errorMessage) {
        document.setStatus(status);
        if (chunkCount != null) {
            document.setChunkCount(chunkCount);
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
