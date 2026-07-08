package com.minyuwei.xhs.coffeeagent.agent.application;

import java.time.Instant;
import java.util.List;

public record RecoverableModelError(
        Code code,
        String category,
        String message,
        boolean recoverable,
        List<String> nextActions,
        String preservedSessionId,
        String retryableMode,
        Instant createdAt
) {
    public enum Code {
        MODEL_TIMEOUT,
        MODEL_AUTH_FAILED,
        MODEL_RATE_LIMITED,
        MODEL_FORMAT_INVALID,
        MODEL_SERVICE_UNAVAILABLE
    }

    public static RecoverableModelError of(Code code, String message, String preservedSessionId, String... nextActions) {
        return new RecoverableModelError(
                code,
                "RETRYABLE",
                message,
                true,
                List.of(nextActions),
                preservedSessionId,
                "openai-gpt55",
                Instant.now()
        );
    }
}
