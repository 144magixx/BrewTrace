package com.minyuwei.xhs.coffeeagent.tasting.domain;

import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;

import java.time.Instant;

/**
 * 保存会话内可追踪的事实或联想状态，并保留模型判断所依据的用户原文证据。
 *
 * @param id 会话内稳定状态项 ID
 * @param status 当前生命周期状态
 * @param boundary 产生当前状态的事实边界
 * @param value 规范化后的事实或联想值
 * @param sourceMessageId 当前状态所依据的用户消息 ID
 * @param sourceQuote 用户消息中的连续原文证据
 * @param reason 模型语义判断理由
 * @param createdAt 状态项首次创建时间
 * @param updatedAt 状态项最近变更时间
 */
public record FactStateItem(
        String id,
        Status status,
        FactUpdate.Boundary boundary,
        String value,
        String sourceMessageId,
        String sourceQuote,
        String reason,
        Instant createdAt,
        Instant updatedAt
) {
    public enum Status {
        CONFIRMED,
        PENDING,
        REJECTED,
        REVISED,
        WITHDRAWN
    }

    /**
     * 创建一项尚未被用户确认的联想。
     *
     * @param id 新状态项 ID
     * @param update 已通过确定性校验的模型增量
     * @param now 状态创建时间
     * @return 待确认状态项
     */
    public static FactStateItem pending(String id, FactUpdate update, Instant now) {
        return new FactStateItem(id, Status.PENDING, update.boundary(), update.value(), update.sourceMessageId(), update.sourceQuote(), update.reason(), now, now);
    }

    /**
     * 创建一项有用户原文证据支持的确认事实。
     *
     * @param id 新状态项 ID
     * @param update 已通过确定性校验的模型增量
     * @param now 状态创建时间
     * @return 已确认事实状态项
     */
    public static FactStateItem confirmed(String id, FactUpdate update, Instant now) {
        return new FactStateItem(id, Status.CONFIRMED, update.boundary(), update.value(), update.sourceMessageId(), update.sourceQuote(), update.reason(), now, now);
    }

    /**
     * 将已有状态项变更为新状态，同时保留首次创建时间和最新用户证据。
     *
     * @param nextStatus 目标生命周期状态
     * @param update 已通过确定性校验的模型增量
     * @param now 状态更新时间
     * @return 变更后的不可变状态项
     */
    public FactStateItem transition(Status nextStatus, FactUpdate update, Instant now) {
        return new FactStateItem(id, nextStatus, update.boundary(), update.value(), update.sourceMessageId(), update.sourceQuote(), update.reason(), createdAt, now);
    }
}

