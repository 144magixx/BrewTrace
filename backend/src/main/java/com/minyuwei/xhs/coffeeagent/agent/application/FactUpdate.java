package com.minyuwei.xhs.coffeeagent.agent.application;

/**
 * 描述主模型根据本轮用户消息识别出的单项事实状态增量。
 *
 * <p>该契约只承载模型语义判断结果；后端仍需验证消息证据、目标项和状态流转后才能应用。</p>
 *
 * @param action 要执行的确定性状态动作
 * @param boundary 模型判定的事实来源边界
 * @param value 规范化后的事实或联想值
 * @param sourceMessageId 提供本轮证据的真实用户消息 ID
 * @param sourceQuote 来源消息中的连续原文片段
 * @param reason 模型作出该语义判断的理由
 * @param targetItemId 被接受、拒绝、修正或撤回的已有状态项 ID；新增项时为 {@code null}
 */
public record FactUpdate(
        Action action,
        Boundary boundary,
        String value,
        String sourceMessageId,
        String sourceQuote,
        String reason,
        String targetItemId
) {
    public enum Action {
        ADD_CONFIRMED_FACT,
        ADD_PENDING_ASSOCIATION,
        ACCEPT_PENDING_ASSOCIATION,
        REJECT_PENDING_ASSOCIATION,
        REVISE_CONFIRMED_FACT,
        WITHDRAW_CONFIRMED_FACT
    }

    public enum Boundary {
        USER_STATED,
        USER_CONFIRMED,
        USER_UNCERTAIN,
        MODEL_INFERRED,
        PENDING_ASSOCIATION,
        USER_REJECTED
    }
}

