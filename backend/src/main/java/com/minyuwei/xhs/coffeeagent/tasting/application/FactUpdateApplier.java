package com.minyuwei.xhs.coffeeagent.tasting.application;

import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;
import com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateChange;
import com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 在整批增量通过校验后执行确定性的会话事实状态流转。
 */
public class FactUpdateApplier {
    /**
     * 按模型响应顺序应用整批合法增量，并为新增状态和审计记录生成稳定 ID。
     *
     * @param session 要更新的会话聚合
     * @param updates 已通过 {@link FactUpdateValidator} 整批校验的增量
     */
    public void apply(TastingSession session, List<FactUpdate> updates) {
        for (FactUpdate update : updates == null ? List.<FactUpdate>of() : updates) {
            applyOne(session, update, Instant.now());
        }
    }

    /**
     * 应用单项已校验动作，不再执行任何自然语言语义推断。
     *
     * @param session 要更新的会话聚合
     * @param update 已校验的模型增量
     * @param now 本次状态流转时间
     */
    private void applyOne(TastingSession session, FactUpdate update, Instant now) {
        if (update.action() == FactUpdate.Action.ADD_CONFIRMED_FACT) {
            add(session, FactStateItem.confirmed(newItemId(), update, now), update, now);
            return;
        }
        if (update.action() == FactUpdate.Action.ADD_PENDING_ASSOCIATION) {
            add(session, FactStateItem.pending(newItemId(), update, now), update, now);
            return;
        }
        FactStateItem previous = session.factStateItem(update.targetItemId()).orElseThrow();
        FactStateItem.Status nextStatus = switch (update.action()) {
            case ACCEPT_PENDING_ASSOCIATION -> FactStateItem.Status.CONFIRMED;
            case REJECT_PENDING_ASSOCIATION -> FactStateItem.Status.REJECTED;
            case REVISE_CONFIRMED_FACT -> FactStateItem.Status.REVISED;
            case WITHDRAW_CONFIRMED_FACT -> FactStateItem.Status.WITHDRAWN;
            default -> throw new IllegalStateException("未支持的事实状态动作: " + update.action());
        };
        FactStateItem transitioned = previous.transition(nextStatus, update, now);
        session.replaceFactState(transitioned, change(update, transitioned.id(), previous.status(), nextStatus, now));
        if (update.action() == FactUpdate.Action.REVISE_CONFIRMED_FACT) {
            FactStateItem replacement = FactStateItem.confirmed(newItemId(), update, now);
            add(session, replacement, update, now);
        }
    }

    /**
     * 将新状态项及其创建审计记录同时加入会话。
     *
     * @param session 要更新的会话聚合
     * @param item 新状态项
     * @param update 产生该状态项的模型增量
     * @param now 创建时间
     */
    private void add(TastingSession session, FactStateItem item, FactUpdate update, Instant now) {
        session.addFactState(item, change(update, item.id(), null, item.status(), now));
    }

    /**
     * 创建状态动作的不可变审计记录。
     *
     * @param update 已应用的模型增量
     * @param targetId 最终关联的状态项 ID
     * @param previousStatus 动作前状态；新增时为空
     * @param nextStatus 动作后状态
     * @param now 动作时间
     * @return 可追加到会话历史的变更记录
     */
    private FactStateChange change(FactUpdate update, String targetId, FactStateItem.Status previousStatus, FactStateItem.Status nextStatus, Instant now) {
        return new FactStateChange("fact-change-" + UUID.randomUUID(), update.action(), targetId, previousStatus, nextStatus,
                update.sourceMessageId(), update.sourceQuote(), update.reason(), now);
    }

    /**
     * 生成不依赖列表顺序的事实状态项 ID。
     *
     * @return 新的会话事实状态项 ID
     */
    private String newItemId() {
        return "fact-state-" + UUID.randomUUID();
    }
}

