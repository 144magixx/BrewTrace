package com.minyuwei.xhs.coffeeagent.memory.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MemoryRecall(
        String id,
        String sessionId,
        String query,
        String resultOwnerType,
        String resultOwnerId,
        double similarityScore,
        List<String> matchedReasons,
        String summary,
        boolean usedInPrompt,
        boolean possibleDuplicate,
        Instant createdAt
) {
    public static MemoryRecall fromEmbedding(String sessionId, String query, MemoryEmbedding embedding, double score, List<String> reasons) {
        return new MemoryRecall(UUID.randomUUID().toString(), sessionId, query, embedding.ownerType().name(), embedding.ownerId(), score, reasons, embedding.contentSummary(), true, score >= 0.80, Instant.now());
    }
}
