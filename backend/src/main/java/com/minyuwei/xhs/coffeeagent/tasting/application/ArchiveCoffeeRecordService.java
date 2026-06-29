package com.minyuwei.xhs.coffeeagent.tasting.application;

import com.minyuwei.xhs.coffeeagent.shared.application.DomainEventOutboxService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.CoffeeRecord;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;
import com.minyuwei.xhs.coffeeagent.user.domain.UserPreference;

import java.util.ArrayList;
import java.util.List;

public class ArchiveCoffeeRecordService {
    private final CurrentUserProvider currentUserProvider;
    private final DomainEventOutboxService outboxService;
    private final List<CoffeeRecord> records = new ArrayList<>();

    public ArchiveCoffeeRecordService(CurrentUserProvider currentUserProvider, DomainEventOutboxService outboxService) {
        this.currentUserProvider = currentUserProvider;
        this.outboxService = outboxService;
    }

    public ArchiveResult archive(String sessionId, String finalDraftId, List<String> flavorKeywords, boolean writeInferredPreferences) {
        CoffeeRecord record = CoffeeRecord.archive(currentUserProvider.currentUserId(), sessionId, finalDraftId, flavorKeywords);
        records.add(record);
        record.pullDomainEvents().forEach(event -> outboxService.enqueue(event, "coffee.record.archived", record.id()));
        List<UserPreference> preferences = writeInferredPreferences
                ? flavorKeywords.stream().limit(2).map(flavor -> UserPreference.inferred(record.userId(), flavor, "来自归档记录 " + record.id())).toList()
                : List.of();
        return new ArchiveResult(record, preferences);
    }

    public List<CoffeeRecord> records() {
        return List.copyOf(records);
    }

    public record ArchiveResult(CoffeeRecord record, List<UserPreference> createdPreferences) {
    }
}
