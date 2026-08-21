package com.tyh.chat.rag.document.service.impl;

import com.tyh.chat.rag.chunk.domain.KnowledgeChunk;
import com.tyh.chat.rag.chunk.service.KnowledgeChunkService;
import com.tyh.chat.rag.document.domain.KnowledgeDocument;
import com.tyh.chat.rag.document.extractor.DocumentTextExtractor;
import com.tyh.chat.rag.document.service.DocumentChunker;
import com.tyh.chat.rag.document.service.KnowledgeDocumentParseService;
import com.tyh.chat.rag.document.service.KnowledgeDocumentService;
import com.tyh.chat.rag.embedding.store.RagEmbeddingStore;
import com.tyh.common.config.ThinkChainConfig;
import com.tyh.common.utils.file.FileUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * 知识库文档解析和切片实现。
 *
 * <p>DocumentTextExtractor 负责从文本、PDF 和 Office 文档中提取文字；本类负责更新处理状态、
 * 清理旧切片并生成新切片。文本按 1200 个字符切片，相邻切片重叠 120 个字符，
 * 重叠可以减少一句话刚好在边界被截断造成的信息丢失。</p>
 */
@Service
public class KnowledgeDocumentParseServiceImpl implements KnowledgeDocumentParseService {

    private final KnowledgeDocumentService documentService;
    private final KnowledgeChunkService chunkService;
    private final ObjectProvider<RagEmbeddingStore> embeddingStoreProvider;
    private final DocumentTextExtractor textExtractor;
    private final DocumentChunker chunker;

    public KnowledgeDocumentParseServiceImpl(KnowledgeDocumentService documentService,
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
            List<DocumentChunker.Chunk> chunks = chunker.split(text,
                    document.getTitle() != null ? document.getTitle() : document.getFileName());
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("Document text is empty");
            }
            // 允许重新解析：先清理旧向量和旧切片，避免同一文档保留两套重复数据。
            embeddingStoreProvider.ifAvailable(store -> store.deleteByDocumentId(document.getId()));
            chunkService.deleteByDocumentId(document.getId());
            for (int i = 0; i < chunks.size(); i++) {
                DocumentChunker.Chunk parsedChunk = chunks.get(i);
                String content = parsedChunk.content();
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setScopeType("KB");
                chunk.setScopeId(document.getKnowledgeBaseId());
                chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
                chunk.setDocumentId(document.getId());
                chunk.setChunkIndex(i);
                chunk.setContent(content);
                // 内容哈希可用于判断内容是否变化；Token 数目前只是按字符数估算，不是模型精确计数。
                chunk.setContentHash(chunker.sha256(content));
                chunk.setTokenCount(chunker.estimateTokens(content));
                chunk.setCharCount(content.length());
                chunk.setPageNumber(parsedChunk.pageNumber());
                chunk.setSectionTitle(parsedChunk.sectionTitle());
                chunk.setEmbeddingStatus("PENDING");
                chunkService.create(chunk);
            }
            // 切片完成后仍属于后台处理中，必须保持 EMBEDDING；只有向量全部写入成功后才由
            // DocumentProcessingService 标记 READY。这样应用在两步之间退出时，重启恢复器能够重新排队。
            mark(document, "EMBEDDING", chunks.size(), null);
        } catch (Exception e) {
            // 解析失败不把异常细节直接返回前端，而是保存到文档记录供服务端排查。
            mark(document, "FAILED", 0, e.getMessage());
        }
        return documentService.getById(documentId);
    }

    private String readText(KnowledgeDocument document) throws Exception {
        String sourceName = document.getFileName() != null ? document.getFileName() : document.getFilePath();
        Path path = resolveLocalPath(document.getFilePath());
        return textExtractor.extract(path, sourceName);
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

    private void mark(KnowledgeDocument document, String status, Integer chunkCount, String errorMessage) {
        document.setStatus(status);
        if (chunkCount != null) {
            document.setChunkCount(chunkCount);
        }
        document.setErrorMessage(errorMessage);
        documentService.update(document);
    }

}
