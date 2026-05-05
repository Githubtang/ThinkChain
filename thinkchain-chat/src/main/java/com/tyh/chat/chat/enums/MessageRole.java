package com.tyh.chat.chat.enums;

import java.util.Locale;

/**
 * 聊天消息角色枚举。
 *
 * @Author: GithubTang
 * @Description: 消息角色
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public enum MessageRole {
    SYSTEM,
    USER,
    ASSISTANT;

    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
