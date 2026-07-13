package com.minyuwei.xhs.coffeeagent.tasting.domain;

import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;

import java.time.Instant;

/**
 * 记录一次已应用的事实状态动作，支持审计修正、撤回和联想确认历史。
 *
 * @param id 变更记录 ID
 * @param action 已执行的状态动作
 * @param targetItemId 被创建或变更的状态项 ID
 * @param previousStatus 变更前状态；新增项时为 {@code null}
 * @param nextStatus 变更后状态
 * @param sourceMessageId 本次动作的用户消息证据 ID
 * @param sourceQuote 用户消息中的连续原文证据
 * @param reason 模型语义判断理由
 * @param createdAt 变更应用时间
 */
public record FactStateChange(
        String id,
        FactUpdate.Action action,
        String targetItemId,
        FactStateItem.Status previousStatus,
        FactStateItem.Status nextStatus,
        String sourceMessageId,
        String sourceQuote,
        String reason,
        Instant createdAt
) {
}

