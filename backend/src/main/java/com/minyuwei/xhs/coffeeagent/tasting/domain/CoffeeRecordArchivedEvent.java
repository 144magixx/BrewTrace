package com.minyuwei.xhs.coffeeagent.tasting.domain;

import com.minyuwei.xhs.coffeeagent.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CoffeeRecordArchivedEvent(
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        Instant occurredAt,
        int schemaVersion,
        Map<String, Object> payload
) implements DomainEvent {
    public static CoffeeRecordArchivedEvent from(CoffeeRecord record) {
        return new CoffeeRecordArchivedEvent(
                UUID.randomUUID().toString(),
                "CoffeeRecordArchivedEvent",
                "CoffeeRecord",
                record.id(),
                Instant.now(),
                1,
                Map.of(
                        "recordId", record.id(),
                        "sessionId", record.sessionId(),
                        "finalDraftId", record.finalDraftId(),
                        "flavorKeywords", record.flavorKeywords(),
                        "source", "USER_ARCHIVE"
                )
        );
    }
}
