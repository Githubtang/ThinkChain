package com.tyh.chat.dto;

/**
 * 多模态内容片段：{@code type} 区分 text、image、file(document)、video、audio；
 * {@link #text} 与 {@link #url} 按类型二选一或组合使用（如 image 的 URL 或 base64）。
 *
 * @Author: GithubTang
 * @Description: 多模态单段内容（type + text/url）
 * @Date: 2026/3/30
 * @Version: 1.0
 */
public class Content {

    /** text / image / file / document / video / audio */
    private String type;

    /** 文本内容，或 image 的 base64 / data URL（与 url 二选一） */
    private String text;

    /** 可访问的资源地址 */
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
