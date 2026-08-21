package com.tyh.chat.rag.document.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库文档和会话文档共用的文本切片组件。
 *
 * <p>先识别 PDF 页码、Excel 工作表、PowerPoint 幻灯片等结构标记，再按段落组合到长度上限；
 * 只有单个段落本身过长时才使用字符重叠切分。这样比直接每 1200 字截断更不容易拆散表格行和段落。</p>
 */
@Component
public class DocumentChunker {

    private static final int CHUNK_SIZE = 1200;
    private static final int CHUNK_OVERLAP = 120;
    private static final Pattern PAGE_MARKER = Pattern.compile("^\\[第\\s*(\\d+)\\s*页]$");
    private static final Pattern SECTION_MARKER = Pattern.compile("^\\[(工作表:\\s*.+|幻灯片\\s+\\d+)]$");

    /**
     * 把提取后的全文转换成带结构信息的切片。
     *
     * @param text           文档提取出的纯文本
     * @param defaultSection 无结构标记时使用的文档标题
     * @return 按原始顺序排列的切片
     */
    public List<Chunk> split(String text, String defaultSection) {
        String normalized = text == null ? "" : text.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        List<Paragraph> paragraphs = toParagraphs(normalized, defaultSection);
        List<Chunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String currentSection = defaultSection;
        Integer currentPage = null;

        for (Paragraph paragraph : paragraphs) {
            if (paragraph.text().length() > CHUNK_SIZE) {
                flush(chunks, current, currentSection, currentPage);
                splitLongParagraph(chunks, paragraph);
                currentSection = paragraph.sectionTitle();
                currentPage = paragraph.pageNumber();
                continue;
            }
            boolean sameSection = equals(currentSection, paragraph.sectionTitle())
                    && equals(currentPage, paragraph.pageNumber());
            int separatorLength = current.isEmpty() ? 0 : 2;
            if (!current.isEmpty() && (!sameSection
                    || current.length() + separatorLength + paragraph.text().length() > CHUNK_SIZE)) {
                flush(chunks, current, currentSection, currentPage);
            }
            if (current.isEmpty()) {
                currentSection = paragraph.sectionTitle();
                currentPage = paragraph.pageNumber();
            } else {
                current.append("\n\n");
            }
            current.append(paragraph.text());
        }
        flush(chunks, current, currentSection, currentPage);
        return List.copyOf(chunks);
    }

    /** 粗略估算 token 数，仅用于后台展示和上下文容量统计。 */
    public int estimateTokens(String text) {
        return Math.max(1, (int) Math.ceil((text == null ? 0 : text.length()) / 4.0D));
    }

    /** 生成稳定内容哈希，便于以后判断切片内容是否发生变化。 */
    public String sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest((text != null ? text : "").getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(bytes);
    }

    private static List<Paragraph> toParagraphs(String text, String defaultSection) {
        List<Paragraph> paragraphs = new ArrayList<>();
        String section = defaultSection;
        Integer page = null;
        StringBuilder paragraph = new StringBuilder();

        for (String rawLine : text.split("\n", -1)) {
            String line = rawLine.stripTrailing();
            Matcher pageMatcher = PAGE_MARKER.matcher(line.trim());
            Matcher sectionMatcher = SECTION_MARKER.matcher(line.trim());
            if (pageMatcher.matches() || sectionMatcher.matches()) {
                addParagraph(paragraphs, paragraph, section, page);
                section = line.trim().substring(1, line.trim().length() - 1);
                page = pageMatcher.matches() ? Integer.valueOf(pageMatcher.group(1)) : null;
                continue;
            }
            if (line.isBlank()) {
                addParagraph(paragraphs, paragraph, section, page);
            } else {
                if (!paragraph.isEmpty()) {
                    paragraph.append('\n');
                }
                paragraph.append(line);
            }
        }
        addParagraph(paragraphs, paragraph, section, page);
        return paragraphs;
    }

    private static void addParagraph(List<Paragraph> paragraphs, StringBuilder value,
                                     String sectionTitle, Integer pageNumber) {
        String text = value.toString().trim();
        if (!text.isBlank()) {
            paragraphs.add(new Paragraph(text, sectionTitle, pageNumber));
        }
        value.setLength(0);
    }

    private static void splitLongParagraph(List<Chunk> chunks, Paragraph paragraph) {
        int start = 0;
        while (start < paragraph.text().length()) {
            int end = Math.min(start + CHUNK_SIZE, paragraph.text().length());
            chunks.add(new Chunk(paragraph.text().substring(start, end).trim(),
                    paragraph.sectionTitle(), paragraph.pageNumber()));
            if (end == paragraph.text().length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
    }

    private static void flush(List<Chunk> chunks, StringBuilder value,
                              String sectionTitle, Integer pageNumber) {
        String text = value.toString().trim();
        if (!text.isBlank()) {
            chunks.add(new Chunk(text, sectionTitle, pageNumber));
        }
        value.setLength(0);
    }

    private static boolean equals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    /** 一条可写入 rag_chunk 表的结构化切片。 */
    public record Chunk(String content, String sectionTitle, Integer pageNumber) {
    }

    private record Paragraph(String text, String sectionTitle, Integer pageNumber) {
    }
}
