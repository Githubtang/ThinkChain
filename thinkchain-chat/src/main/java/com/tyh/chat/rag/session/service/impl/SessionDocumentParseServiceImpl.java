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
            embeddingStoreProvider.ifAvailable(store -> store.deleteByDocumentId(document.getId()));
            chunkService.deleteByDocumentId(document.getId());
            int tokenCount = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String content = chunks.get(i);
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
