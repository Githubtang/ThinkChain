package com.tyh.chat.rag.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识文档可由用户修改的字段。
 *
 * <p>文件路径、处理状态、切片数量和归属信息只能由服务端处理链路修改。</p>
 */
public class KnowledgeDocumentUpdateRequest {

    @NotBlank(message = "文档标题不能为空")
    @Size(max = 255, message = "文档标题长度不能超过255个字符")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
