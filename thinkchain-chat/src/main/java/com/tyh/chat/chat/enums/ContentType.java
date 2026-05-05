package com.tyh.chat.chat.enums;

import java.util.Locale;

/**
 * 多模态内容类型枚举。
 *
 * @Author: GithubTang
 * @Description: 内容类型
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public enum ContentType {
    TEXT,
    IMAGE,
    DOCUMENT,
    VIDEO,
    AUDIO;

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
