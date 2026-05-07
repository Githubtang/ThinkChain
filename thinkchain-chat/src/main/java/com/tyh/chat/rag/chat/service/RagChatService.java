package com.tyh.chat.rag.chat.service;

import com.tyh.chat.rag.chat.dto.RagChatRequest;
import com.tyh.chat.rag.chat.dto.RagChatResponse;

public interface RagChatService {

    RagChatResponse chat(RagChatRequest request);
}
