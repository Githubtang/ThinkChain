package com.tyh.chat.validation;

import com.tyh.common.config.ThinkChainConfig;
import com.tyh.common.constant.Constants;
import com.tyh.common.constant.HttpStatus;
import com.tyh.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * 把数据库中的上传访问路径安全地转换成服务器本地路径。
 *
 * <p>数据库通常保存 {@code /profile/upload/...}，真正文件位于 {@link ThinkChainConfig#getProfile()}
 * 下。本组件统一完成去前缀、规范化和根目录校验，防止 {@code ..}、绝对路径或符号链接跳出上传目录。</p>
 */
@Component
public class ChatFilePathResolver {

    /**
     * 解析一个必须存在且可读取的上传文件，供 PDF/Office/文本解析使用。
     *
     * @param storedPath 数据库存储的访问路径
     * @return 已验证位于 profile 根目录中的真实文件路径
     */
    public Path resolveReadableFile(String storedPath) {
        Path target = resolveInsideProfile(storedPath);
        try {
            Path root = profileRoot().toRealPath();
            Path realTarget = target.toRealPath();
            if (!realTarget.startsWith(root)
                    || !Files.isRegularFile(realTarget, LinkOption.NOFOLLOW_LINKS)) {
                throw invalidPath();
            }
            return realTarget;
        } catch (IOException exception) {
            throw new ServiceException("上传文件不存在或不可读取", HttpStatus.BAD_REQUEST)
                    .setDetailMessage(exception.getMessage());
        }
    }

    /**
     * 解析待删除路径。目标文件可以已经不存在，但其父目录不能通过符号链接跳出 profile 根目录。
     */
    public Path resolveDeletablePath(String storedPath) {
        Path target = resolveInsideProfile(storedPath);
        try {
            Path root = profileRoot().toRealPath();
            Path parent = target.getParent();
            if (parent != null && Files.exists(parent) && !parent.toRealPath().startsWith(root)) {
                throw invalidPath();
            }
            return target;
        } catch (IOException exception) {
            throw new ServiceException("上传文件路径不可访问", HttpStatus.BAD_REQUEST)
                    .setDetailMessage(exception.getMessage());
        }
    }

    private Path resolveInsideProfile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            throw new ServiceException("上传文件路径不能为空", HttpStatus.BAD_REQUEST);
        }
        String relative = storedPath.trim().replace('\\', '/');
        if (relative.startsWith(Constants.RESOURCE_PREFIX + "/")) {
            relative = relative.substring(Constants.RESOURCE_PREFIX.length() + 1);
        } else if (relative.startsWith("/") || relative.startsWith("//")
                || relative.matches("(?i)^[a-z]:/.*")) {
            // 只有项目生成的 /profile/... 是合法绝对访问路径，磁盘绝对路径一律拒绝。
            throw invalidPath();
        }
        Path relativePath;
        try {
            relativePath = Path.of(relative);
        } catch (RuntimeException exception) {
            throw invalidPath();
        }
        if (relativePath.isAbsolute()) {
            throw invalidPath();
        }
        for (Path part : relativePath) {
            if ("..".equals(part.toString())) {
                throw invalidPath();
            }
        }
        Path root = profileRoot();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw invalidPath();
        }
        return target;
    }

    private static Path profileRoot() {
        return Path.of(ThinkChainConfig.getProfile()).toAbsolutePath().normalize();
    }

    private static ServiceException invalidPath() {
        return new ServiceException("上传文件路径不合法", HttpStatus.BAD_REQUEST);
    }
}
