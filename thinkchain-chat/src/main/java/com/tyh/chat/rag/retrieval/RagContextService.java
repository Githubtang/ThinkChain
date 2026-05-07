package com.tyh.chat.rag.retrieval;

import com.tyh.chat.chat.dto.ChatRequest;

public interface RagContextService {

    ChatRequest augment(ChatRequest request);
}
