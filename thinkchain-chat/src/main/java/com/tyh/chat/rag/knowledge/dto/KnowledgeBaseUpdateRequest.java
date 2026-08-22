package com.tyh.chat.rag.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改知识库时允许客户端填写的业务字段。
 *
 * <p>本 DTO 不包含资源归属和统计字段，避免请求体覆盖服务端维护的数据。</p>
 */
public class KnowledgeBaseUpdateRequest {

    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称长度不能超过100个字符")
    private String name;

    @Size(max = 500, message = "知识库描述长度不能超过500个字符")
    private String description;

    @Pattern(regexp = "ACTIVE|DISABLED", message = "知识库状态不合法")
    private String status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
