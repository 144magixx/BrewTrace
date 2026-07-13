package com.minyuwei.xhs.coffeeagent.agent.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record ModelAgentMessage(
        ModelMessageType messageType,
        String talk,
        PostModelMessage post,
        ConversationModelMessage conversation,
        List<FactUpdate> factUpdates,
        List<String> warnings,
        Instant generatedAt
) {
    public ModelAgentMessage {
        factUpdates = factUpdates == null ? List.of() : List.copyOf(factUpdates);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }

    /**
     * 校验模型消息路由结构及事实增量的基础数量约束，不执行证据或状态流转校验。
     *
     * @return 模型响应结构错误；空集合表示可进入应用层事实增量校验
     */
    public List<String> validationErrors() {
        List<String> errors = new ArrayList<>();
        if (messageType == null) {
            errors.add("缺少 messageType");
        }
        if (talk == null || talk.isBlank()) {
            errors.add("缺少 talk");
        }
        if (factUpdates.size() > 20) {
            errors.add("factUpdates 不能超过 20 项");
        }
        if (messageType == ModelMessageType.CONVERSATION) {
            if (conversation == null) {
                errors.add("CONVERSATION 缺少 conversation");
            } else if (conversation.questions().size() != 1) {
                errors.add("CONVERSATION 必须恰好包含 1 个追问");
            } else if (conversation.answerOptions().size() > 4) {
                errors.add("CONVERSATION 备选答案不能超过 4 个");
            } else if (conversation.answerOptions().stream().anyMatch(this::invalidAnswerOption)) {
                errors.add("CONVERSATION 备选答案必须包含 id、label 和 content");
            }
            if (post != null && !post.variants().isEmpty()) {
                errors.add("CONVERSATION 不能携带 post 草稿");
            }
        }
        if (messageType == ModelMessageType.POST) {
            if (post == null) {
                errors.add("POST 缺少 post");
            } else {
                errors.addAll(CopyVariant.validateCompleteSet(post.variants()));
            }
            if (conversation != null && (!conversation.questions().isEmpty() || !conversation.answerOptions().isEmpty())) {
                errors.add("POST 不能携带 conversation 追问");
            }
        }
        return errors;
    }

    /**
     * 判断备选回答是否缺少模型消息契约要求的展示或提交字段。
     *
     * @param option 待检查的备选回答
     * @return 任一必填字段缺失时返回 {@code true}
     */
    private boolean invalidAnswerOption(ConversationModelMessage.AnswerOption option) {
        return option == null
                || option.id() == null || option.id().isBlank()
                || option.label() == null || option.label().isBlank()
                || option.content() == null || option.content().isBlank();
    }

    /**
     * 以统一方式读取 POST 文案变体，CONVERSATION 消息返回空集合。
     *
     * @return POST 文案变体或空集合
     */
    public List<CopyVariant> variants() {
        return post == null ? List.of() : post.variants();
    }
}
