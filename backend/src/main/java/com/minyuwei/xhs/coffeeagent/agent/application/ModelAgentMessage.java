package com.minyuwei.xhs.coffeeagent.agent.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record ModelAgentMessage(
        ModelMessageType messageType,
        String talk,
        PostModelMessage post,
        ConversationModelMessage conversation,
        List<String> warnings,
        Instant generatedAt
) {
    public ModelAgentMessage {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }

    public List<String> validationErrors() {
        List<String> errors = new ArrayList<>();
        if (messageType == null) {
            errors.add("缺少 messageType");
        }
        if (talk == null || talk.isBlank()) {
            errors.add("缺少 talk");
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

    private boolean invalidAnswerOption(ConversationModelMessage.AnswerOption option) {
        return option == null
                || option.id() == null || option.id().isBlank()
                || option.label() == null || option.label().isBlank()
                || option.content() == null || option.content().isBlank();
    }

    public List<CopyVariant> variants() {
        return post == null ? List.of() : post.variants();
    }
}
