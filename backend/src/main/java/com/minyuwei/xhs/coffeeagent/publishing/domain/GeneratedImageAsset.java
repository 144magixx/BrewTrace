package com.minyuwei.xhs.coffeeagent.publishing.domain;

import com.minyuwei.xhs.coffeeagent.shared.domain.SourceType;

import java.time.Instant;
import java.util.UUID;

public record GeneratedImageAsset(
        String id,
        String sessionId,
        String draftId,
        String filePath,
        String prompt,
        String modelName,
        SourceType sourceType,
        Instant createdAt
) {
    public static GeneratedImageAsset create(String sessionId, String draftId, String prompt) {
        String id = UUID.randomUUID().toString();
        return new GeneratedImageAsset(id, sessionId, draftId, ".local-storage/" + sessionId + "/generated/" + id + ".png", prompt, "gpt-image-2", SourceType.MODEL_SUGGESTED, Instant.now());
    }
}
