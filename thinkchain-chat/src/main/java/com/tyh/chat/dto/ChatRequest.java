package com.tyh.chat.dto;

import java.util.List;

/**
 * 统一对话请求 DTO：包含逻辑模型名与多轮/多模态 {@link Message} 列表，与具体厂商 SDK 无关。
 *
 * @Author: GithubTang
 * @Description: 对话请求载体（model + messages）
 * @Date: 2026/3/30
 * @Version: 1.0
 */
public class ChatRequest {

    /** 配置中的逻辑模型名，对应 {@code application-ai.yml} 的 name */
    private String model;

    private List<Message> messages;

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }
}
