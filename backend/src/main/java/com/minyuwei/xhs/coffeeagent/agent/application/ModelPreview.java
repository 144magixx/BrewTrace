package com.minyuwei.xhs.coffeeagent.agent.application;

import java.time.Instant;

public final class ModelPreview {
    private ModelPreview() {
    }

    public record ModelRequestPreview(
            String label,
            String modelName,
            String mode,
            String endpointPath,
            String rawJson,
            String redactionStatus,
            Instant sentAt
    ) {
    }

    public record ModelResponsePreview(
            String label,
            String modelName,
            String mode,
            String rawJson,
            String redactionStatus,
            Instant receivedAt
    ) {
    }

    public record SensitiveRedactionResult(
            String target,
            String checkedPatterns,
            boolean redacted,
            boolean safeToDisplay,
            Instant checkedAt
    ) {
    }
}
