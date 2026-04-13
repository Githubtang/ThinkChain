package com.tyh.chat.dto;

import java.util.List;

/**
 * 单条对话消息：角色 + 多段多模态 {@link Content}（text、image、document 等）。
 *
 * @Author: GithubTang
 * @Description: 对话消息（role + contents）
 * @Date: 2026/3/30
 * @Version: 1.0
 */
public class Message {

    /** user / assistant / system */
    private String role;

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
