package com.minyuwei.xhs.coffeeagent.tasting.domain;

import com.minyuwei.xhs.coffeeagent.shared.domain.SourceType;

import java.time.Instant;
import java.util.UUID;

public record ConversationMessage(
        String id,
        String sessionId,
        Role role,
        String content,
        SourceType sourceType,
        Instant createdAt
) {
    public static ConversationMessage user(String sessionId, String content) {
        return new ConversationMessage(UUID.randomUUID().toString(), sessionId, Role.USER, content, SourceType.USER_CONFIRMED, Instant.now());
    }

    public static ConversationMessage assistant(String sessionId, String content) {
        return new ConversationMessage(UUID.randomUUID().toString(), sessionId, Role.ASSISTANT, content, SourceType.MODEL_SUGGESTED, Instant.now());
    }

    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM
    }
}
