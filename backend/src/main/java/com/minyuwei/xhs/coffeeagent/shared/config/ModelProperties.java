package com.minyuwei.xhs.coffeeagent.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coffee-agent.model")
public record ModelProperties(
        String mode,
        String textModel,
        String imageModel,
        String baseUrl,
        int timeoutSeconds,
        int maxRetries,
        String apiKey
) {
    public static ModelProperties defaults() {
        return new ModelProperties("openai-gpt55", "gpt-5.5", "gpt-image-2", "https://saturday.sankuai.com/v1", 120, 2, "");
    }
}
