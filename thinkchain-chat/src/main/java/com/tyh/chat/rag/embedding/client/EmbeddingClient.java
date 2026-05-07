package com.tyh.chat.rag.embedding.client;

public interface EmbeddingClient {

    String modelName();

    float[] embed(String text);
}
