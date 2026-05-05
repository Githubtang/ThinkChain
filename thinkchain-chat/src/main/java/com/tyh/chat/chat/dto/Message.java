package com.tyh.chat.chat.dto;

import java.util.List;

/**
 * 聊天消息对象，包含角色和多模态内容片段。
 *
 * @Author: GithubTang
 * @Description: 聊天消息
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class Message {

    /** 角色类型：user / assistant / system。 */
    private String role;

    /** 多模态内容列表。 */
    private List<Content> contents;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }
}
