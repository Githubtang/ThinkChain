package com.tyh.chat.validation;

import com.tyh.chat.rag.document.extractor.DocumentTextExtractor;
import com.tyh.common.config.ThinkChainConfig;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.exception.ServiceException;
import com.tyh.common.utils.file.FileUploadUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * 聊天文档上传的统一校验与保存组件。
 *
 * <p>白名单直接复用 DocumentTextExtractor 支持的扩展名，包含文本、PDF、Word、Excel 和 PowerPoint；
 * 同时限制文件名、路径字符和 10MB 大小，避免控制器各自实现出不同规则。</p>
 */
@Component
public class ChatFileValidator {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final String[] ALLOWED_EXTENSION_ARRAY = DocumentTextExtractor.supportedExtensions();

    public String upload(MultipartFile file) {
        // 必须先通过业务白名单校验，之后才允许 FileUploadUtils 写入磁盘。
        validate(file);
        try {
            return FileUploadUtils.upload(ThinkChainConfig.getUploadPath(), file, ALLOWED_EXTENSION_ARRAY);
        } catch (Exception exception) {
            throw new ServiceException("文件上传失败", HttpStatus.ERROR)
                    .setDetailMessage(exception.getMessage());
        }
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空", HttpStatus.BAD_REQUEST);
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new ServiceException("文件名不能为空", HttpStatus.BAD_REQUEST);
        }
        // getName 会移除目录部分；两者不同说明客户端文件名中夹带了路径。
        if (!originalName.equals(FilenameUtils.getName(originalName))) {
            throw new ServiceException("文件名包含非法路径", HttpStatus.BAD_REQUEST);
        }
        if (originalName.length() > FileUploadUtils.DEFAULT_FILE_NAME_LENGTH) {
            throw new ServiceException("文件名长度不能超过" + FileUploadUtils.DEFAULT_FILE_NAME_LENGTH,
                    HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ServiceException("文件大小不能超过10MB", HttpStatus.BAD_REQUEST);
        }
        String extension = FilenameUtils.getExtension(originalName).toLowerCase(Locale.ROOT);
        if (!DocumentTextExtractor.supportsExtension(extension)) {
            throw new ServiceException("不支持的文件类型: " + extension, HttpStatus.UNSUPPORTED_TYPE);
        }
    }
}
