package com.tyh.chat.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
    @NotBlank(message = "消息角色不能为空")
    @Pattern(regexp = "(?i)user|assistant|system", message = "消息角色不合法")
    private String role;

    /** 多模态内容列表。 */
    @NotEmpty(message = "消息内容不能为空")
    @Size(max = 20, message = "单条消息内容片段不能超过20个")
    @Valid
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
