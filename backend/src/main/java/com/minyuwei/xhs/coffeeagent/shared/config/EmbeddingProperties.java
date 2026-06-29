package com.minyuwei.xhs.coffeeagent.shared.config;

public record EmbeddingProperties(String model, int dimensions) {
    public static EmbeddingProperties defaults() {
        return new EmbeddingProperties("text-embedding-v4", 1024);
    }
}
