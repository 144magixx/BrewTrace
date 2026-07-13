package com.minyuwei.xhs.coffeeagent.tasting.application;

import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem;
import com.minyuwei.xhs.coffeeagent.tasting.infrastructure.TastingSessionRepositoryAdapter;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FactStateUpdateTest {
    private TastingSessionApplicationService service;
    private String sessionId;
    private String messageId;

    @BeforeEach
    void setUp() {
        service = new TastingSessionApplicationService(new TastingSessionRepositoryAdapter(), new CurrentUserProvider());
        sessionId = service.createSession(OrchestrationMode.EXPLICIT_WORKFLOW).id();
        messageId = service.recordUserMessage(sessionId, "我喝到柑橘感，但可能也有一点红茶；不是坚果味。").id();
    }

    @Test
    void appliesUserStatedFactWithExactEvidence() {
        var result = service.applyFactUpdates(sessionId, List.of(update(
                FactUpdate.Action.ADD_CONFIRMED_FACT, FactUpdate.Boundary.USER_STATED,
                "风味：柑橘", "柑橘感", null)));

        assertTrue(result.applied());
        assertEquals(List.of("风味：柑橘"), service.workspace(sessionId).confirmedFacts().stream().map(FactStateItem::value).toList());
    }

    @Test
    void keepsUncertainAndModelInferredValuesPending() {
        assertTrue(service.applyFactUpdates(sessionId, List.of(
                update(FactUpdate.Action.ADD_PENDING_ASSOCIATION, FactUpdate.Boundary.USER_UNCERTAIN, "风味：红茶", "可能也有一点红茶", null),
                update(FactUpdate.Action.ADD_PENDING_ASSOCIATION, FactUpdate.Boundary.MODEL_INFERRED, "联想：佛手柑", "柑橘感", null)
        )).applied());

        assertEquals(2, service.workspace(sessionId).pendingAssociations().size());
        assertTrue(service.workspace(sessionId).confirmedFacts().isEmpty());
    }

    @Test
    void acceptsAndRejectsExistingPendingAssociations() {
        service.applyFactUpdates(sessionId, List.of(
                update(FactUpdate.Action.ADD_PENDING_ASSOCIATION, FactUpdate.Boundary.USER_UNCERTAIN, "风味：红茶", "可能也有一点红茶", null),
                update(FactUpdate.Action.ADD_PENDING_ASSOCIATION, FactUpdate.Boundary.MODEL_INFERRED, "风味：坚果", "不是坚果味", null)
        ));
        List<FactStateItem> pending = service.workspace(sessionId).pendingAssociations();

        assertTrue(service.applyFactUpdates(sessionId, List.of(
                update(FactUpdate.Action.ACCEPT_PENDING_ASSOCIATION, FactUpdate.Boundary.USER_CONFIRMED, "风味：红茶", "红茶", pending.get(0).id()),
                update(FactUpdate.Action.REJECT_PENDING_ASSOCIATION, FactUpdate.Boundary.USER_REJECTED, "风味：坚果", "不是坚果味", pending.get(1).id())
        )).applied());

        assertEquals(1, service.workspace(sessionId).confirmedFacts().size());
        assertEquals(1, service.workspace(sessionId).rejectedAssociations().size());
    }

    @Test
    void revisesAndWithdrawsConfirmedFactsWithHistory() {
        service.applyFactUpdates(sessionId, List.of(update(
                FactUpdate.Action.ADD_CONFIRMED_FACT, FactUpdate.Boundary.USER_STATED, "风味：柑橘", "柑橘感", null)));
        String originalId = service.workspace(sessionId).confirmedFacts().getFirst().id();

        assertTrue(service.applyFactUpdates(sessionId, List.of(update(
                FactUpdate.Action.REVISE_CONFIRMED_FACT, FactUpdate.Boundary.USER_CONFIRMED, "风味：橙皮", "柑橘感", originalId))).applied());
        String replacementId = service.workspace(sessionId).confirmedFacts().getFirst().id();
        assertTrue(service.applyFactUpdates(sessionId, List.of(update(
                FactUpdate.Action.WITHDRAW_CONFIRMED_FACT, FactUpdate.Boundary.USER_REJECTED, "风味：橙皮", "不是坚果味", replacementId))).applied());

        assertTrue(service.workspace(sessionId).confirmedFacts().isEmpty());
        assertEquals(4, service.workspace(sessionId).factStateChanges().size());
    }

    @Test
    void rejectsMissingMessageInvalidQuoteIllegalTransitionAndInferredConfirmedFact() {
        FactUpdate validPending = update(FactUpdate.Action.ADD_PENDING_ASSOCIATION, FactUpdate.Boundary.USER_UNCERTAIN, "风味：红茶", "可能也有一点红茶", null);
        assertTrue(service.applyFactUpdates(sessionId, List.of(validPending)).applied());
        String pendingId = service.workspace(sessionId).pendingAssociations().getFirst().id();

        assertFalse(service.applyFactUpdates(sessionId, List.of(new FactUpdate(
                FactUpdate.Action.ADD_CONFIRMED_FACT, FactUpdate.Boundary.USER_STATED, "风味：柑橘", "missing", "柑橘感", "理由", null))).applied());
        assertFalse(service.applyFactUpdates(sessionId, List.of(new FactUpdate(
                FactUpdate.Action.ADD_CONFIRMED_FACT, FactUpdate.Boundary.USER_STATED, "风味：柑橘", messageId, "不存在的原文", "理由", null))).applied());
        assertFalse(service.applyFactUpdates(sessionId, List.of(update(
                FactUpdate.Action.WITHDRAW_CONFIRMED_FACT, FactUpdate.Boundary.USER_REJECTED, "风味：红茶", "红茶", pendingId))).applied());
        assertFalse(service.applyFactUpdates(sessionId, List.of(update(
                FactUpdate.Action.ADD_CONFIRMED_FACT, FactUpdate.Boundary.MODEL_INFERRED, "联想：佛手柑", "柑橘感", null))).applied());
    }

    @Test
    void rejectsDuplicateAndConflictingUpdatesAtomically() {
        FactUpdate update = update(FactUpdate.Action.ADD_CONFIRMED_FACT, FactUpdate.Boundary.USER_STATED, "风味：柑橘", "柑橘感", null);
        assertFalse(service.applyFactUpdates(sessionId, List.of(update, update)).applied());
        assertTrue(service.workspace(sessionId).confirmedFacts().isEmpty());

        service.applyFactUpdates(sessionId, List.of(update(
                FactUpdate.Action.ADD_PENDING_ASSOCIATION, FactUpdate.Boundary.USER_UNCERTAIN, "风味：红茶", "红茶", null)));
        String pendingId = service.workspace(sessionId).pendingAssociations().getFirst().id();
        assertFalse(service.applyFactUpdates(sessionId, List.of(
                update(FactUpdate.Action.ACCEPT_PENDING_ASSOCIATION, FactUpdate.Boundary.USER_CONFIRMED, "风味：红茶", "红茶", pendingId),
                update(FactUpdate.Action.REJECT_PENDING_ASSOCIATION, FactUpdate.Boundary.USER_REJECTED, "风味：红茶", "不是坚果味", pendingId)
        )).applied());
        assertEquals(FactStateItem.Status.PENDING, service.workspace(sessionId).pendingAssociations().getFirst().status());
    }

    private FactUpdate update(FactUpdate.Action action, FactUpdate.Boundary boundary, String value, String quote, String targetId) {
        return new FactUpdate(action, boundary, value, messageId, quote, "测试模型语义判断理由", targetId);
    }
}

