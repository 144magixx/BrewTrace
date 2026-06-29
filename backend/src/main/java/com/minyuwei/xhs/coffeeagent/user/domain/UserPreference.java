package com.minyuwei.xhs.coffeeagent.user.domain;

import java.time.Instant;
import java.util.UUID;

public record UserPreference(
        String id,
        String userId,
        PreferenceType preferenceType,
        String value,
        String evidence,
        double confidence,
        String source,
        Status status,
        Instant updatedAt
) {
    public static UserPreference inferred(String userId, String value, String evidence) {
        return new UserPreference(UUID.randomUUID().toString(), userId, PreferenceType.FLAVOR, value, evidence, 0.72, "ARCHIVED_RECORD", Status.CANDIDATE, Instant.now());
    }

    public enum PreferenceType {
        FLAVOR,
        COPY_STYLE,
        BREWING
    }

    public enum Status {
        CANDIDATE,
        ACCEPTED,
        DELETED
    }
}
