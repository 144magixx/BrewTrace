package com.minyuwei.xhs.coffeeagent.shared.config;

public record CoffeeAgentProperties(
        ModelProperties model,
        EmbeddingProperties embedding,
        XiaohongshuProperties xiaohongshu,
        StorageProperties storage
) {
    public static CoffeeAgentProperties defaults() {
        return new CoffeeAgentProperties(
                ModelProperties.defaults(),
                EmbeddingProperties.defaults(),
                XiaohongshuProperties.defaults(),
                StorageProperties.defaults()
        );
    }
}
