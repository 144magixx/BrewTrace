package com.minyuwei.xhs.coffeeagent.shared.infrastructure.kafka;

import com.minyuwei.xhs.coffeeagent.shared.application.DomainEventOutboxService;

public class OutboxPublisher {
    private final DomainEventOutboxService outboxService;

    public OutboxPublisher(DomainEventOutboxService outboxService) {
        this.outboxService = outboxService;
    }

    public int publishPending() {
        var records = outboxService.pendingRecords();
        records.forEach(record -> outboxService.markPublished(record.event().eventId()));
        return records.size();
    }
}
