package com.minyuwei.xhs.coffeeagent.agent.application;

import java.util.List;

public record ConversationModelMessage(
        List<String> questions,
        List<AnswerOption> answerOptions,
        List<CopyVariant.FactUsage> pendingConfirmations,
        List<String> warnings
) {
    public ConversationModelMessage {
        questions = questions == null ? List.of() : List.copyOf(questions);
        answerOptions = answerOptions == null ? List.of() : List.copyOf(answerOptions);
        pendingConfirmations = pendingConfirmations == null ? List.of() : List.copyOf(pendingConfirmations);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public record AnswerOption(
            String id,
            String label,
            String content
    ) {
    }
}
