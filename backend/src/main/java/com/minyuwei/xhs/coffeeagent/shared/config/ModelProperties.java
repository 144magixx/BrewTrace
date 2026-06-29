package com.minyuwei.xhs.coffeeagent.shared.config;

public record ModelProperties(String textModel, String imageModel, int maxRetries) {
    public static ModelProperties defaults() {
        return new ModelProperties("gpt-5.5", "gpt-image-2", 2);
    }
}
