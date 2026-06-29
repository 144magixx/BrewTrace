package com.minyuwei.xhs.coffeeagent.shared.domain;

import java.time.Instant;
import java.util.Map;

public interface DomainEvent {
    String eventId();

    String eventType();

    String aggregateType();

    String aggregateId();

    Instant occurredAt();

    int schemaVersion();

    Map<String, Object> payload();
}
