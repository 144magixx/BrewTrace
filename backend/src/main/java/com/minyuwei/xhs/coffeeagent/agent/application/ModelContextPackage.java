package com.minyuwei.xhs.coffeeagent.agent.application;

import java.time.Instant;
import java.util.List;

/**
 * 表示主模型调用前后端已保存的会话状态快照；该类型永远不是模型输出。
 *
 * @param sessionId 当前会话 ID
 * @param mode 模型模式
 * @param currentSession 带稳定消息 ID 的会话上下文
 * @param confirmedFacts 已验证并持久化的确认事实
 * @param pendingAssociations 等待用户确认的联想
 * @param candidateMemoryBoundaries 真实记忆召回候选；未接入时为空
 * @param excludedItems 明确禁止模型使用的状态项
 * @param promptConstraints 从版本化资源加载的动态约束
 * @param createdAt 快照组装时间
 */
public record ModelContextPackage(
        String sessionId,
        ModelMode mode,
        List<ContextEntry> currentSession,
        List<ContextEntry> confirmedFacts,
        List<ContextEntry> pendingAssociations,
        List<ContextEntry> candidateMemoryBoundaries,
        List<ContextEntry> excludedItems,
        List<String> promptConstraints,
        Instant createdAt
) {
    /**
     * 描述可追踪的模型输入项，证据字段允许模型准确引用用户消息原文。
     *
     * @param id 当前输入项的稳定 ID
     * @param content 输入项内容
     * @param sourceLabel 来源边界标签
     * @param sendStatus 发送边界
     * @param exclusionReason 排除原因；未排除时为空
     * @param sourceMessageId 原始用户消息 ID；无用户消息来源时为空
     * @param sourceQuote 已保存的连续原文证据；会话消息通常与 content 相同
     * @param reason 状态项的模型判断理由；原始消息可为空
     * @param state 当前事实状态；普通会话消息可为空
     * @param role 会话消息角色，普通状态项为空；只用于恢复多轮对话语义，不改变事实边界
     */
    public record ContextEntry(
            String id,
            String content,
            String sourceLabel,
            String sendStatus,
            String exclusionReason,
            String sourceMessageId,
            String sourceQuote,
            String reason,
            String state,
            String role
    ) {
        /**
         * 保留尚未提供会话角色的完整上下文构造方式。
         *
         * @param id 当前输入项的稳定 ID
         * @param content 输入项内容
         * @param sourceLabel 来源边界标签
         * @param sendStatus 发送边界
         * @param exclusionReason 排除原因
         * @param sourceMessageId 原始消息 ID
         * @param sourceQuote 原始连续证据
         * @param reason 状态项判断理由
         * @param state 当前事实状态
         */
        public ContextEntry(
                String id,
                String content,
                String sourceLabel,
                String sendStatus,
                String exclusionReason,
                String sourceMessageId,
                String sourceQuote,
                String reason,
                String state
        ) {
            this(id, content, sourceLabel, sendStatus, exclusionReason, sourceMessageId, sourceQuote, reason, state, null);
        }

        /**
         * 保留旧调用方的基础上下文构造方式；无法提供证据时相关字段保持为空。
         *
         * @param id 当前输入项的稳定 ID
         * @param content 输入项内容
         * @param sourceLabel 来源边界标签
         * @param sendStatus 发送边界
         * @param exclusionReason 排除原因
         */
        public ContextEntry(String id, String content, String sourceLabel, String sendStatus, String exclusionReason) {
            this(id, content, sourceLabel, sendStatus, exclusionReason, null, null, null, null, null);
        }
    }
}
