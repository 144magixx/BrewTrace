package com.minyuwei.xhs.coffeeagent.workbench.api;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.agent.application.ConversationModelMessage;
import com.minyuwei.xhs.coffeeagent.agent.application.CopyVariant;
import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMessageType;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelPreview;
import com.minyuwei.xhs.coffeeagent.agent.application.PostModelMessage;
import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;
import com.minyuwei.xhs.coffeeagent.shared.domain.ConfirmationStatus;
import com.minyuwei.xhs.coffeeagent.shared.domain.SourceType;
import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.workbench.domain.AgentStateModels;
import com.minyuwei.xhs.coffeeagent.workbench.domain.WebWorkbenchSession;

import java.time.Instant;
import java.util.List;

public final class WebWorkbenchDtos {
    private WebWorkbenchDtos() {
    }

    public record CreateSessionRequest(OrchestrationMode mode) {
    }

    public record SubmitMessageRequest(String content, String modelMode) {
    }

    public record ClearSessionRequest(boolean confirmed) {
    }

    public record WorkbenchSnapshot(
            String sessionId,
            WebWorkbenchSession.Status status,
            String heroQuestion,
            OrchestrationMode orchestrationMode,
            List<WebConversationMessage> conversation,
            RecordSummary recordSummary,
            List<DraftTab> draftTabs,
            AgentStateSnapshot agentState,
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

    public record AgentStateSnapshot(
            List<AgentStatusCard> statusCards,
            List<ContextItem> contextItems,
            List<ConfirmedFact> confirmedFacts,
            List<PendingAssociation> pendingAssociations,
            List<CandidateMemory> candidateMemories,
            ContextPreview contextPreview,
            ModelModeSnapshot modelMode,
            ModelOutputSnapshot modelOutput,
            List<FactBoundaryCheckResult> factBoundaryChecks,
            CapabilityBoundary capabilityBoundary,
            SessionControlAction sessionControlAction,
            Instant updatedAt
    ) {
    }

    public record AgentStatusCard(
            String id,
            AgentStateModels.AgentCardType type,
            String title,
            String summary,
            String sourceLabel,
            AgentStateModels.SendStatus sendStatus,
            AgentStateModels.RiskLevel riskLevel,
            Instant createdAt
    ) {
    }

    public record ContextItem(
            String id,
            String role,
            String content,
            SourceType sourceType,
            ConfirmationStatus confirmationStatus,
            AgentStateModels.SendStatus sendStatus,
            String sourceMessageId,
            Instant createdAt
    ) {
    }

    public record ConfirmedFact(
            String id,
            String factType,
            String value,
            String sourceContextItemId,
            String sourceMessageId,
            String sourceQuote,
            String reason,
            FactUpdate.Boundary boundary,
            ConfirmationStatus confirmationStatus,
            AgentStateModels.SendStatus sendStatus
    ) {
        /**
         * 保留旧工作台测试和调用方的简化构造方式。
         *
         * @param id 事实 ID
         * @param factType 事实展示类型
         * @param value 事实值
         * @param sourceContextItemId 来源上下文项 ID
         * @param confirmationStatus 确认状态
         * @param sendStatus 发送状态
         */
        public ConfirmedFact(String id, String factType, String value, String sourceContextItemId,
                             ConfirmationStatus confirmationStatus, AgentStateModels.SendStatus sendStatus) {
            this(id, factType, value, sourceContextItemId, null, null, null, FactUpdate.Boundary.USER_CONFIRMED,
                    confirmationStatus, sendStatus);
        }
    }

    public record PendingAssociation(
            String id,
            String value,
            String triggerFactId,
            String sourceMessageId,
            String sourceQuote,
            String reason,
            FactUpdate.Boundary boundary,
            ConfirmationStatus confirmationStatus,
            AgentStateModels.SendStatus sendStatus
    ) {
        /**
         * 保留旧工作台测试和调用方的简化待确认联想构造方式。
         *
         * @param id 联想 ID
         * @param value 联想值
         * @param triggerFactId 触发事实 ID
         * @param reason 联想理由
         * @param confirmationStatus 确认状态
         * @param sendStatus 发送状态
         */
        public PendingAssociation(String id, String value, String triggerFactId, String reason,
                                  ConfirmationStatus confirmationStatus, AgentStateModels.SendStatus sendStatus) {
            this(id, value, triggerFactId, null, null, reason, FactUpdate.Boundary.PENDING_ASSOCIATION,
                    confirmationStatus, sendStatus);
        }
    }

    public record CandidateMemory(
            String id,
            String title,
            String content,
            String sourceBoundary,
            String reason,
            String relationType,
            String similarityLabel,
            String conflictStatus,
            AgentStateModels.SendStatus sendStatus
    ) {
    }

    public record ContextPreview(
            List<ContextPreviewSection> sections,
            int willSendCount,
            int excludedCount,
            String boundaryNote,
            ModelPreview.ModelRequestPreview requestPreview,
            ModelPreview.ModelResponsePreview responsePreview
    ) {
    }

    public record ContextPreviewSection(
            String sectionType,
            String title,
            List<ContextPreviewItem> items
    ) {
    }

    public record ContextPreviewItem(
            String content,
            String sourceLabel,
            AgentStateModels.SendStatus sendStatus,
            String exclusionReason
    ) {
    }

    public record ModelOutputSnapshot(
            String outputType,
            String mode,
            String modelName,
            String statusLabel,
            String content,
            String sourceBoundary,
            ModelMessageType messageType,
            String talk,
            PostModelMessage post,
            ConversationModelMessage conversation,
            List<FactUpdate> factUpdates,
            List<String> warnings,
            List<CopyVariant> variants,
            ModelPreview.ModelRequestPreview requestPreview,
            ModelPreview.ModelResponsePreview responsePreview,
            RecoverableModelError recoverableError,
            Instant generatedAt
    ) {
        public ModelOutputSnapshot(String outputType, String content, String sourceBoundary, Instant generatedAt) {
            this(outputType, "openai-gpt55", "gpt-5.5", sourceBoundary, content, sourceBoundary, null, "", null, null, List.of(), List.of(), List.of(), null, null, null, generatedAt);
        }
    }

    public record ModelModeSnapshot(
            String mode,
            String displayName,
            String modelName,
            String baseUrlLabel,
            boolean available,
            boolean requiresApiKey,
            String statusLabel,
            boolean fallbackAvailable
    ) {
    }

    public record FactBoundaryCheckResult(
            String id,
            String expression,
            AgentStateModels.BasisType basisType,
            AgentStateModels.RiskLevel riskLevel,
            String sourceReference,
            String message,
            AgentStateModels.RecommendedAction recommendedAction
    ) {
    }

    public record CapabilityBoundary(
            boolean realModelConnected,
            boolean longTermMemoryConnected,
            boolean xiaohongshuConnected,
            String message
    ) {
    }

    public record SessionControlAction(
            String actionType,
            boolean confirmationRequired,
            String impactSummary,
            boolean confirmed,
            String resultStatus
    ) {
    }
}
