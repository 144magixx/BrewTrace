package com.minyuwei.xhs.coffeeagent.shared.application;

import com.minyuwei.xhs.coffeeagent.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DomainEventOutboxService {
    private final List<OutboxRecord> records = new ArrayList<>();

    public OutboxRecord enqueue(DomainEvent event, String topic, String partitionKey) {
        OutboxRecord record = new OutboxRecord(event, topic, partitionKey, OutboxStatus.PENDING, 0, Instant.now(), null);
        records.add(record);
        return record;
    }

    public List<OutboxRecord> pendingRecords() {
        return records.stream().filter(record -> record.status() == OutboxStatus.PENDING).toList();
    }

    public void markPublished(String eventId) {
        replace(eventId, OutboxStatus.PUBLISHED, null);
    }

    public void markRetryable(String eventId, String error) {
        replace(eventId, OutboxStatus.FAILED_RETRYABLE, error);
    }

    private void replace(String eventId, OutboxStatus status, String error) {
        for (int i = 0; i < records.size(); i++) {
            OutboxRecord record = records.get(i);
            if (record.event().eventId().equals(eventId)) {
                records.set(i, record.withStatus(status, error));
            }
        }
    }

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED_RETRYABLE,
        FAILED_DEAD
    }

    public record OutboxRecord(
            DomainEvent event,
            String topic,
            String partitionKey,
            OutboxStatus status,
            int retryCount,
            Instant nextRetryAt,
            String lastError
    ) {
        OutboxRecord withStatus(OutboxStatus status, String error) {
            return new OutboxRecord(event, topic, partitionKey, status, retryCount + (error == null ? 0 : 1), nextRetryAt, error);
        }
    }
}
