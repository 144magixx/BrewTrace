package com.minyuwei.xhs.coffeeagent.tasting.domain;

import com.minyuwei.xhs.coffeeagent.shared.domain.AggregateRoot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class CoffeeRecord extends AggregateRoot {
    private final String id;
    private final String userId;
    private final String sessionId;
    private final String finalDraftId;
    private final List<String> flavorKeywords;
    private final Instant createdAt;
    private String possibleDuplicateOf;

    private CoffeeRecord(String id, String userId, String sessionId, String finalDraftId, List<String> flavorKeywords) {
        this.id = id;
        this.userId = userId;
        this.sessionId = sessionId;
        this.finalDraftId = finalDraftId;
        this.flavorKeywords = List.copyOf(flavorKeywords);
        this.createdAt = Instant.now();
        registerEvent(CoffeeRecordArchivedEvent.from(this));
    }

    public static CoffeeRecord archive(String userId, String sessionId, String finalDraftId, List<String> flavorKeywords) {
        return new CoffeeRecord(UUID.randomUUID().toString(), userId, sessionId, finalDraftId, flavorKeywords);
    }

    public String summary() {
        return "咖啡记录：" + String.join("、", flavorKeywords);
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String sessionId() {
        return sessionId;
    }

    public String finalDraftId() {
        return finalDraftId;
    }

    public List<String> flavorKeywords() {
        return flavorKeywords;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String possibleDuplicateOf() {
        return possibleDuplicateOf;
    }

    public void markPossibleDuplicateOf(String recordId) {
        this.possibleDuplicateOf = recordId;
    }
}
