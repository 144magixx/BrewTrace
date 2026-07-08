package com.minyuwei.xhs.coffeeagent.agent.application;

import java.time.Instant;
import java.util.List;

public record ModelContextPackage(
        String sessionId,
        ModelMode mode,
        List<ContextEntry> currentSession,
        List<ContextEntry> confirmedFacts,
        List<ContextEntry> pendingAssociations,
        List<ContextEntry> candidateMemoryBoundaries,
        List<ContextEntry> excludedItems,
        List<String> promptConstraints,
        Instant createdAt
) {
    public record ContextEntry(
            String id,
            String content,
            String sourceLabel,
            String sendStatus,
            String exclusionReason
    ) {
    }
}
