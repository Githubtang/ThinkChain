package com.tyh.chat.rag.chat.dto;

import com.tyh.chat.chat.dto.ChatResponse;

import java.util.List;

public class RagChatResponse {

    private String answer;
    private List<RagSource> sources;
    private ChatResponse chatResponse;
    private Long elapsedMs;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<RagSource> getSources() {
        return sources;
    }

    public void setSources(List<RagSource> sources) {
        this.sources = sources;
    }

    public ChatResponse getChatResponse() {
        return chatResponse;
    }

    public void setChatResponse(ChatResponse chatResponse) {
        this.chatResponse = chatResponse;
    }

    public Long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(Long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }
}
