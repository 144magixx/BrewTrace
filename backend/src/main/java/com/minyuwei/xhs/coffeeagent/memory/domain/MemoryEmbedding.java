package com.minyuwei.xhs.coffeeagent.memory.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MemoryEmbedding(
        String id,
        OwnerType ownerType,
        String ownerId,
        EmbeddingType embeddingType,
        String modelName,
        int dimensions,
        String contentSummary,
        Map<String, Object> metadata,
        List<Double> vector,
        Instant createdAt
) {
    public static MemoryEmbedding ofRecord(String recordId, String summary, List<Double> vector) {
        return new MemoryEmbedding(UUID.randomUUID().toString(), OwnerType.COFFEE_RECORD, recordId, EmbeddingType.SUMMARY, "text-embedding-v4", vector.size(), summary, Map.of(), List.copyOf(vector), Instant.now());
    }

    public enum OwnerType {
        COFFEE_RECORD,
        DRAFT_COPY,
        USER_PREFERENCE,
        EXTERNAL_REFERENCE
    }

    public enum EmbeddingType {
        SUMMARY,
        COPY,
        PREFERENCE
    }
}
