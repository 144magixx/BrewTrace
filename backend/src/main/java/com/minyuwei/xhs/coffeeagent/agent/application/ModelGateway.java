package com.minyuwei.xhs.coffeeagent.agent.application;

import java.time.Instant;
import java.util.List;

public interface ModelGateway {
    ModelResult complete(ModelContextPackage contextPackage);

    record ModelResult(
            ModelMode mode,
            String outputType,
            String modelName,
            String statusLabel,
            String sourceBoundary,
            String content,
            ModelMessageType messageType,
            String talk,
            PostModelMessage post,
            ConversationModelMessage conversation,
            List<String> warnings,
            List<CopyVariant> variants,
            ModelPreview.ModelRequestPreview requestPreview,
            ModelPreview.ModelResponsePreview responsePreview,
            RecoverableModelError recoverableError,
            Instant generatedAt
    ) {
        public boolean succeeded() {
            return recoverableError == null;
        }
    }

    record ModelRequest(String purpose, String prompt, List<String> facts, List<String> boundaries) {
    }
}
