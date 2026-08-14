package com.tyh.chat.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 多模态内容片段：文本、图片、文件、文档、视频或音频。
 *
 * @Author: GithubTang
 * @Description: 多模态内容片段
 * @Date: 2026/4/29
 * @Version: 1.0
 */
public class Content {

    /** 内容类型：text / image / file / document / video / audio。 */
    @NotBlank(message = "内容类型不能为空")
    @Pattern(regexp = "(?i)text|image|file|document|video|audio", message = "内容类型不支持")
    private String type;

    /** 文本内容，或图片场景下的 base64/data URL 数据。 */
    @Size(max = 2_000_000, message = "内容文本过长")
    private String text;

    /** 可公开访问的资源 URL。 */
    @Size(max = 4096, message = "资源URL长度不能超过4096个字符")
    private String url;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
