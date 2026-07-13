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
    private final List<FactStateItem> factStateItems = new ArrayList<>();
    private final List<FactStateChange> factStateChanges = new ArrayList<>();

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

    public ConversationMessage addUserMessage(String content) {
        ConversationMessage message = ConversationMessage.user(id, content);
        messages.add(message);
        updatedAt = Instant.now();
        return message;
    }

    public ConversationMessage addAssistantMessage(String content) {
        ConversationMessage message = ConversationMessage.assistant(id, content);
        messages.add(message);
        updatedAt = Instant.now();
        return message;
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

    /**
     * 返回当前仍有效的确认事实文本，不再从用户消息关键词推断事实。
     *
     * @return 已通过模型增量校验和状态流转后保存的确认事实值
     */
    public List<String> confirmedFacts() {
        return confirmedFactItems().stream().map(FactStateItem::value).toList();
    }

    /**
     * 返回当前仍有效的已确认事实状态项。
     *
     * @return 状态为 {@link FactStateItem.Status#CONFIRMED} 的不可变快照
     */
    public List<FactStateItem> confirmedFactItems() {
        return factStateItems.stream().filter(item -> item.status() == FactStateItem.Status.CONFIRMED).toList();
    }

    /**
     * 返回当前等待用户确认的联想状态项。
     *
     * @return 状态为 {@link FactStateItem.Status#PENDING} 的不可变快照
     */
    public List<FactStateItem> pendingAssociationItems() {
        return factStateItems.stream().filter(item -> item.status() == FactStateItem.Status.PENDING).toList();
    }

    /**
     * 返回已经被用户拒绝的联想，供审计和避免重复建议使用。
     *
     * @return 状态为 {@link FactStateItem.Status#REJECTED} 的不可变快照
     */
    public List<FactStateItem> rejectedAssociationItems() {
        return factStateItems.stream().filter(item -> item.status() == FactStateItem.Status.REJECTED).toList();
    }

    /**
     * 按稳定 ID 查询任意生命周期中的事实状态项。
     *
     * @param itemId 目标状态项 ID
     * @return 命中的状态项；不存在时为空
     */
    public java.util.Optional<FactStateItem> factStateItem(String itemId) {
        return factStateItems.stream().filter(item -> item.id().equals(itemId)).findFirst();
    }

    /**
     * 新增一项事实状态，并记录状态变更历史。
     *
     * @param item 已完成确定性校验的新状态项
     * @param change 与该新增动作对应的审计记录
     */
    public void addFactState(FactStateItem item, FactStateChange change) {
        factStateItems.add(item);
        factStateChanges.add(change);
        updatedAt = change.createdAt();
    }

    /**
     * 替换已有事实状态，并追加不可变变更记录。
     *
     * @param item 已完成合法状态流转的新状态项
     * @param change 描述本次流转的审计记录
     * @throws IllegalArgumentException 当目标状态项已不存在时抛出，防止静默创建错误状态
     */
    public void replaceFactState(FactStateItem item, FactStateChange change) {
        for (int index = 0; index < factStateItems.size(); index++) {
            if (factStateItems.get(index).id().equals(item.id())) {
                factStateItems.set(index, item);
                factStateChanges.add(change);
                updatedAt = change.createdAt();
                return;
            }
        }
        throw new IllegalArgumentException("事实状态项不存在: " + item.id());
    }

    /**
     * 返回全部事实状态，包含拒绝、修正和撤回后的历史项。
     *
     * @return 事实状态不可变快照
     */
    public List<FactStateItem> factStateItems() {
        return List.copyOf(factStateItems);
    }

    /**
     * 返回按应用顺序保存的事实状态变更历史。
     *
     * @return 状态变更不可变快照
     */
    public List<FactStateChange> factStateChanges() {
        return List.copyOf(factStateChanges);
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
