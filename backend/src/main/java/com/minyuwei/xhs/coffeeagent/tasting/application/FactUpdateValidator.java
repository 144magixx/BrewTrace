package com.minyuwei.xhs.coffeeagent.tasting.application;

import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;
import com.minyuwei.xhs.coffeeagent.tasting.domain.ConversationMessage;
import com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对模型事实增量执行不涉及咖啡语义的确定性协议、证据和状态流转校验。
 */
public class FactUpdateValidator {
    private static final int MAX_UPDATES = 20;
    private static final int MAX_VALUE_LENGTH = 500;
    private static final int MAX_QUOTE_LENGTH = 500;
    private static final int MAX_REASON_LENGTH = 1000;

    /**
     * 校验整批模型事实增量；任一项失败时整批不应应用，避免部分状态污染。
     *
     * @param session 持有真实消息和当前事实状态的会话
     * @param updates 模型本轮返回的类型化状态增量
     * @return 所有确定性校验错误；空集合表示整批可应用
     */
    public List<String> validate(TastingSession session, List<FactUpdate> updates) {
        List<FactUpdate> safeUpdates = updates == null ? List.of() : updates;
        List<String> errors = new ArrayList<>();
        if (safeUpdates.size() > MAX_UPDATES) {
            errors.add("factUpdates 不能超过 " + MAX_UPDATES + " 项");
            return List.copyOf(errors);
        }
        Map<String, ConversationMessage> userMessages = userMessages(session);
        Set<String> updateKeys = new HashSet<>();
        Map<String, Set<FactUpdate.Action>> targetActions = new HashMap<>();
        for (int index = 0; index < safeUpdates.size(); index++) {
            FactUpdate update = safeUpdates.get(index);
            String prefix = "factUpdates[" + index + "] ";
            if (update == null) {
                errors.add(prefix + "不能为空");
                continue;
            }
            validateRequiredFields(update, prefix, errors);
            validateEvidence(update, prefix, userMessages, errors);
            validateActionBoundary(update, prefix, errors);
            validateTarget(session, update, prefix, errors);
            String updateKey = update.action() + "|" + normalized(update.targetItemId()) + "|" + normalized(update.value());
            if (!updateKeys.add(updateKey)) {
                errors.add(prefix + "与同一响应中的其他更新重复");
            }
            if (update.targetItemId() != null && !update.targetItemId().isBlank()) {
                Set<FactUpdate.Action> actions = targetActions.computeIfAbsent(update.targetItemId(), ignored -> new HashSet<>());
                actions.add(update.action());
                if (actions.size() > 1) {
                    errors.add(prefix + "与同一目标项的其他更新冲突");
                }
            }
        }
        return List.copyOf(errors);
    }

    /**
     * 建立仅包含真实用户消息的证据索引，确保助手文本不能作为确认事实证据。
     *
     * @param session 当前会话
     * @return 以消息 ID 为键的用户消息索引
     */
    private Map<String, ConversationMessage> userMessages(TastingSession session) {
        Map<String, ConversationMessage> result = new HashMap<>();
        session.messages().stream()
                .filter(message -> message.role() == ConversationMessage.Role.USER)
                .forEach(message -> result.put(message.id(), message));
        return result;
    }

    /**
     * 校验增量的结构必填项和长度上限，不解释字段中的自然语言语义。
     *
     * @param update 待校验增量
     * @param prefix 错误定位前缀
     * @param errors 收集错误的可变列表
     */
    private void validateRequiredFields(FactUpdate update, String prefix, List<String> errors) {
        if (update.action() == null) errors.add(prefix + "缺少 action");
        if (update.boundary() == null) errors.add(prefix + "缺少 boundary");
        requireText(update.value(), MAX_VALUE_LENGTH, prefix + "value", errors);
        requireText(update.sourceMessageId(), 100, prefix + "sourceMessageId", errors);
        requireText(update.sourceQuote(), MAX_QUOTE_LENGTH, prefix + "sourceQuote", errors);
        requireText(update.reason(), MAX_REASON_LENGTH, prefix + "reason", errors);
    }

    /**
     * 验证来源消息存在且原文证据是该用户消息中的连续片段。
     *
     * @param update 待校验增量
     * @param prefix 错误定位前缀
     * @param userMessages 真实用户消息索引
     * @param errors 收集错误的可变列表
     */
    private void validateEvidence(FactUpdate update, String prefix, Map<String, ConversationMessage> userMessages, List<String> errors) {
        if (update.sourceMessageId() == null || update.sourceQuote() == null) return;
        ConversationMessage source = userMessages.get(update.sourceMessageId());
        if (source == null) {
            errors.add(prefix + "sourceMessageId 不对应真实用户消息");
        } else if (!source.content().contains(update.sourceQuote())) {
            errors.add(prefix + "sourceQuote 不是来源消息中的连续原文片段");
        }
    }

    /**
     * 校验动作与事实边界的协议组合，阻止推断和不确定内容直接升级为确认事实。
     *
     * @param update 待校验增量
     * @param prefix 错误定位前缀
     * @param errors 收集错误的可变列表
     */
    private void validateActionBoundary(FactUpdate update, String prefix, List<String> errors) {
        if (update.action() == null || update.boundary() == null) return;
        switch (update.action()) {
            case ADD_CONFIRMED_FACT -> {
                if (update.boundary() != FactUpdate.Boundary.USER_STATED && update.boundary() != FactUpdate.Boundary.USER_CONFIRMED) {
                    errors.add(prefix + "新增确认事实只能使用 USER_STATED 或 USER_CONFIRMED 边界");
                }
            }
            case ADD_PENDING_ASSOCIATION -> {
                if (update.boundary() != FactUpdate.Boundary.USER_UNCERTAIN
                        && update.boundary() != FactUpdate.Boundary.MODEL_INFERRED
                        && update.boundary() != FactUpdate.Boundary.PENDING_ASSOCIATION) {
                    errors.add(prefix + "新增待确认联想的边界不合法");
                }
            }
            case ACCEPT_PENDING_ASSOCIATION, REVISE_CONFIRMED_FACT -> {
                if (update.boundary() != FactUpdate.Boundary.USER_CONFIRMED) {
                    errors.add(prefix + "确认或修正动作必须使用 USER_CONFIRMED 边界");
                }
            }
            case REJECT_PENDING_ASSOCIATION, WITHDRAW_CONFIRMED_FACT -> {
                if (update.boundary() != FactUpdate.Boundary.USER_REJECTED) {
                    errors.add(prefix + "拒绝或撤回动作必须使用 USER_REJECTED 边界");
                }
            }
        }
    }

    /**
     * 验证目标项存在及其当前状态满足动作前置条件。
     *
     * @param session 当前会话
     * @param update 待校验增量
     * @param prefix 错误定位前缀
     * @param errors 收集错误的可变列表
     */
    private void validateTarget(TastingSession session, FactUpdate update, String prefix, List<String> errors) {
        if (update.action() == null) return;
        boolean createAction = update.action() == FactUpdate.Action.ADD_CONFIRMED_FACT
                || update.action() == FactUpdate.Action.ADD_PENDING_ASSOCIATION;
        if (createAction) {
            if (update.targetItemId() != null && !update.targetItemId().isBlank()) {
                errors.add(prefix + "新增动作不能指定 targetItemId");
            }
            return;
        }
        if (update.targetItemId() == null || update.targetItemId().isBlank()) {
            errors.add(prefix + "更新已有项必须提供 targetItemId");
            return;
        }
        FactStateItem target = session.factStateItem(update.targetItemId()).orElse(null);
        if (target == null) {
            errors.add(prefix + "targetItemId 不存在");
            return;
        }
        switch (update.action()) {
            case ACCEPT_PENDING_ASSOCIATION, REJECT_PENDING_ASSOCIATION -> {
                if (target.status() != FactStateItem.Status.PENDING) errors.add(prefix + "目标项不是待确认状态");
            }
            case REVISE_CONFIRMED_FACT, WITHDRAW_CONFIRMED_FACT -> {
                if (target.status() != FactStateItem.Status.CONFIRMED) errors.add(prefix + "目标项不是已确认状态");
            }
            default -> { }
        }
    }

    /**
     * 对必填文本执行空值和长度校验。
     *
     * @param value 待校验文本
     * @param maxLength 最大允许长度
     * @param field 错误中使用的字段名
     * @param errors 收集错误的可变列表
     */
    private void requireText(String value, int maxLength, String field, List<String> errors) {
        if (value == null || value.isBlank()) errors.add(field + " 不能为空");
        else if (value.length() > maxLength) errors.add(field + " 超过长度上限");
    }

    /**
     * 为重复检测提供不含首尾空白的稳定文本。
     *
     * @param value 可为空的文本
     * @return 空值对应空字符串，否则返回去除首尾空白后的文本
     */
    private String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}

