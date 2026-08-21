package com.tyh.chat.rag.document.extractor;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.ShapeContainer;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.sl.usermodel.TableCell;
import org.apache.poi.sl.usermodel.TableShape;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 把上传文档转换为可切片、可向量化的纯文本。
 *
 * <p>该组件只负责“读取文件内容”，不负责创建切片、写数据库或调用模型。知识库文档和会话文档
 * 共用它，避免两套解析规则不一致。实现使用直接的 switch 分支，每种文件格式对应一种明确读取方式：</p>
 * <ul>
 *     <li>PDF 使用 PDFBox 提取页面文字；</li>
 *     <li>doc/docx、xls/xlsx、ppt/pptx 使用 Apache POI；</li>
 *     <li>原有文本类格式继续按 UTF-8 读取。</li>
 * </ul>
 *
 * <p>这里只提取文档中已有的文字，不执行图片 OCR；扫描版 PDF 如果没有文本层会返回明确错误。</p>
 */
@Component
public class DocumentTextExtractor {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "csv", "json", "xml", "html", "htm",
            "log", "sql", "java", "js", "ts", "css", "yml", "yaml", "properties");
    private static final Set<String> OFFICE_AND_PDF_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx");

    /**
     * 判断扩展名是否受当前解析器支持，供上传校验器复用同一份白名单。
     *
     * @param extension 不带点号的文件扩展名
     * @return 支持时返回 true
     */
    public static boolean supportsExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return TEXT_EXTENSIONS.contains(normalized) || OFFICE_AND_PDF_EXTENSIONS.contains(normalized);
    }

    /** 返回上传组件使用的完整扩展名白名单副本。 */
    public static String[] supportedExtensions() {
        return Stream.concat(TEXT_EXTENSIONS.stream(), OFFICE_AND_PDF_EXTENSIONS.stream())
                .toArray(String[]::new);
    }

    /**
     * 根据文件扩展名选择解析方式并返回统一纯文本。
     *
     * @param path       服务器磁盘上的文件路径
     * @param sourceName 用户上传时的原始文件名，用于识别扩展名
     * @return 已规范换行的纯文本
     * @throws Exception 文件损坏、加密、格式不受支持或没有可提取文字时抛出
     */
    public String extract(Path path, String sourceName) throws Exception {
        if (path == null || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Document file does not exist: " + path);
        }
        String extension = FilenameUtils.getExtension(sourceName != null ? sourceName : path.getFileName().toString());
        extension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);

        String text;
        try {
            text = switch (extension) {
                case "pdf" -> extractPdf(path);
                case "doc" -> extractDoc(path);
                case "docx" -> extractDocx(path);
                case "xls", "xlsx" -> extractWorkbook(path);
                case "ppt", "pptx" -> extractSlides(path);
                default -> {
                    if (!TEXT_EXTENSIONS.contains(extension)) {
                        throw new UnsupportedOperationException("Unsupported document type: " + extension);
                    }
                    yield Files.readString(path, StandardCharsets.UTF_8);
                }
            };
        } catch (InvalidPasswordException | EncryptedDocumentException exception) {
            throw new IllegalArgumentException("暂不支持加密文档，请移除打开密码后重新上传", exception);
        }

        String normalized = normalize(text);
        if (normalized.isBlank()) {
            if ("pdf".equals(extension)) {
                throw new IllegalArgumentException("PDF中没有可提取文字，扫描版PDF需要先进行OCR");
            }
            throw new IllegalArgumentException("Document text is empty");
        }
        return normalized;
    }

    /** 使用 PDFBox 按页面顺序提取 PDF 文本层。 */
    private static String extractPdf(Path path) throws Exception {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                // 显式写入页码标记，后续 DocumentChunker 才能把 page_number 保存到切片元数据。
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                text.append("[第 ").append(page).append(" 页]\n")
                        .append(stripper.getText(document).trim())
                        .append("\n\n");
            }
            return text.toString();
        }
    }

    /** 读取 Word 97-2003 二进制 doc 文档。 */
    private static String extractDoc(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path);
             HWPFDocument document = new HWPFDocument(input);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    /** 读取 Office Open XML 格式的 docx 文档。 */
    private static String extractDocx(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(input);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * 读取 xls/xlsx 的工作表和单元格显示值。
     * 每个工作表增加名称标记，单元格使用制表符分隔，便于向量检索时保留表格语义。
     */
    private static String extractWorkbook(Path path) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(path.toFile())) {
            DataFormatter formatter = new DataFormatter(Locale.SIMPLIFIED_CHINESE);
            // 不主动计算公式，只读取文件中保存的缓存结果，避免解析文档时执行复杂或外部引用公式。
            formatter.setUseCachedValuesForFormulaCells(true);
            StringBuilder text = new StringBuilder();
            for (Sheet sheet : workbook) {
                text.append("[工作表: ").append(sheet.getSheetName()).append("]\n");
                for (Row row : sheet) {
                    int lastCell = row.getLastCellNum();
                    if (lastCell < 0) {
                        continue;
                    }
                    StringBuilder rowText = new StringBuilder();
                    for (int column = 0; column < lastCell; column++) {
                        if (column > 0) {
                            rowText.append('\t');
                        }
                        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        if (cell != null) {
                            rowText.append(formatter.formatCellValue(cell));
                        }
                    }
                    String value = rowText.toString().stripTrailing();
                    if (!value.isBlank()) {
                        text.append(value).append('\n');
                    }
                }
                text.append('\n');
            }
            return text.toString();
        }
    }

    /** 读取 ppt/pptx 中每一页可见的文本框、组合形状和表格文字。 */
    private static String extractSlides(Path path) throws Exception {
        try (SlideShow<?, ?> slideShow = SlideShowFactory.create(path.toFile())) {
            StringBuilder text = new StringBuilder();
            int slideNumber = 1;
            for (Slide<?, ?> slide : slideShow.getSlides()) {
                text.append("[幻灯片 ").append(slideNumber++).append("]\n");
                for (Shape<?, ?> shape : slide.getShapes()) {
                    appendSlideShape(shape, text);
                }
                text.append('\n');
            }
            return text.toString();
        }
    }

    /** 递归提取幻灯片形状；表格按行列输出，组合形状继续读取其子形状。 */
    private static void appendSlideShape(Shape<?, ?> shape, StringBuilder text) {
        if (shape instanceof TableShape<?, ?> table) {
            for (int row = 0; row < table.getNumberOfRows(); row++) {
                StringBuilder rowText = new StringBuilder();
                for (int column = 0; column < table.getNumberOfColumns(); column++) {
                    if (column > 0) {
                        rowText.append('\t');
                    }
                    TableCell<?, ?> cell = table.getCell(row, column);
                    if (cell != null && cell.getText() != null) {
                        rowText.append(cell.getText().trim());
                    }
                }
                appendBlock(text, rowText.toString());
            }
            return;
        }
        if (shape instanceof TextShape<?, ?> textShape) {
            appendBlock(text, textShape.getText());
            return;
        }
        if (shape instanceof ShapeContainer<?, ?> container) {
            for (Object child : container.getShapes()) {
                if (child instanceof Shape<?, ?> childShape) {
                    appendSlideShape(childShape, text);
                }
            }
        }
    }

    /** 追加非空文本块并保证块间有换行。 */
    private static void appendBlock(StringBuilder target, String value) {
        if (value != null && !value.isBlank()) {
            target.append(value.trim()).append('\n');
        }
    }

    /** 统一 Windows/Unix 换行，去除空字符并压缩连续空白行。 */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u0000', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t ]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
