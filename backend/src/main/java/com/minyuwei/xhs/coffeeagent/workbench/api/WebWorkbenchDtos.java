package com.minyuwei.xhs.coffeeagent.workbench.api;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.shared.domain.SourceType;
import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.workbench.domain.WebWorkbenchSession;

import java.time.Instant;
import java.util.List;

public final class WebWorkbenchDtos {
    private WebWorkbenchDtos() {
    }

    public record CreateSessionRequest(OrchestrationMode mode) {
    }

    public record SubmitMessageRequest(String content) {
    }

    public record WorkbenchSnapshot(
            String sessionId,
            WebWorkbenchSession.Status status,
            String heroQuestion,
            OrchestrationMode orchestrationMode,
            List<WebConversationMessage> conversation,
            RecordSummary recordSummary,
            List<DraftTab> draftTabs,
            ApiError lastError,
            Instant updatedAt
    ) {
    }

    public record WebConversationMessage(
            String id,
            String role,
            String content,
            SourceType sourceType,
            Instant createdAt
    ) {
    }

    public record RecordSummary(
            List<String> confirmedFacts,
            List<String> pendingQuestions,
            List<String> suggestedFlavors,
            String draftStatus,
            List<String> factBoundaryNotes
    ) {
    }

    public record DraftTab(
            String draftId,
            String style,
            String title,
            String body,
            List<String> tags,
            List<String> factBoundaryNotes,
            List<String> reviewWarnings
    ) {
    }
}
