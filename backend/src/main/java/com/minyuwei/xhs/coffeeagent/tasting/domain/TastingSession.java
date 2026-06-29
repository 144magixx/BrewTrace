package com.minyuwei.xhs.coffeeagent.tasting.domain;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftCopy;
import com.minyuwei.xhs.coffeeagent.shared.domain.AggregateRoot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TastingSession extends AggregateRoot {
    private final String id;
    private final String userId;
    private Status status;
    private String currentIntent;
    private OrchestrationMode orchestrationMode;
    private final Instant createdAt;
    private Instant updatedAt;
    private String activeDraftId;
    private final List<ConversationMessage> messages = new ArrayList<>();
    private final List<DraftCopy> drafts = new ArrayList<>();

    private TastingSession(String id, String userId, OrchestrationMode orchestrationMode) {
        this.id = id;
        this.userId = userId;
        this.status = Status.ACTIVE;
        this.currentIntent = "COLLECT_TASTING_FACTS";
        this.orchestrationMode = orchestrationMode;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public static TastingSession create(String userId, OrchestrationMode orchestrationMode) {
        return new TastingSession(UUID.randomUUID().toString(), userId, orchestrationMode);
    }

    public void addUserMessage(String content) {
        messages.add(ConversationMessage.user(id, content));
        updatedAt = Instant.now();
    }

    public void addAssistantMessage(String content) {
        messages.add(ConversationMessage.assistant(id, content));
        updatedAt = Instant.now();
    }

    public void addDrafts(List<DraftCopy> generatedDrafts) {
        drafts.addAll(generatedDrafts);
        if (!generatedDrafts.isEmpty()) {
            activeDraftId = generatedDrafts.getFirst().id();
            status = Status.READY_TO_ARCHIVE;
        }
        updatedAt = Instant.now();
    }

    public String latestUserContent() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage message = messages.get(i);
            if (message.role() == ConversationMessage.Role.USER) {
                return message.content();
            }
        }
        return "";
    }

    public List<String> confirmedFacts() {
        String combined = String.join(" ", messages.stream().filter(message -> message.role() == ConversationMessage.Role.USER).map(ConversationMessage::content).toList());
        List<String> facts = new ArrayList<>();
        if (combined.contains("水洗")) {
            facts.add("处理法：水洗");
        }
        if (combined.contains("埃塞")) {
            facts.add("产地：埃塞");
        }
        if (combined.contains("柑橘")) {
            facts.add("用户确认风味：柑橘");
        }
        if (combined.contains("红茶")) {
            facts.add("用户确认风味：红茶");
        }
        if (combined.contains("92") || combined.contains("水温")) {
            facts.add("已补充冲煮参数");
        }
        return facts;
    }

    public String id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public Status status() {
        return status;
    }

    public OrchestrationMode orchestrationMode() {
        return orchestrationMode;
    }

    public void switchMode(OrchestrationMode orchestrationMode) {
        this.orchestrationMode = orchestrationMode;
        this.updatedAt = Instant.now();
    }

    public List<ConversationMessage> messages() {
        return List.copyOf(messages);
    }

    public List<DraftCopy> drafts() {
        return List.copyOf(drafts);
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public String activeDraftId() {
        return activeDraftId;
    }

    public enum Status {
        ACTIVE,
        READY_TO_ARCHIVE,
        ARCHIVED,
        PAUSED
    }
}
