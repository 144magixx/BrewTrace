package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.agent.application.CopyVariant;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMessageType;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftCopy;
import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.ConversationMessage;
import com.minyuwei.xhs.coffeeagent.workbench.api.WebWorkbenchDtos;
import com.minyuwei.xhs.coffeeagent.workbench.domain.WebWorkbenchSession;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class WebWorkbenchService {
    private static final String HERO_QUESTION = "今天喝了什么咖啡？";
    private final TastingSessionApplicationService tastingSessionService;
    private final AgentStateAssembler agentStateAssembler;
    private final ConcurrentMap<String, ModelGateway.ModelResult> lastModelResults = new ConcurrentHashMap<>();

    public WebWorkbenchService(TastingSessionApplicationService tastingSessionService, AgentStateAssembler agentStateAssembler) {
        this.tastingSessionService = tastingSessionService;
        this.agentStateAssembler = agentStateAssembler;
    }

    public WebWorkbenchDtos.WorkbenchSnapshot emptySnapshot() {
        return new WebWorkbenchDtos.WorkbenchSnapshot(
                null,
                WebWorkbenchSession.Status.EMPTY,
                HERO_QUESTION,
                OrchestrationMode.EXPLICIT_WORKFLOW,
                List.of(),
                new WebWorkbenchDtos.RecordSummary(List.of(), List.of(), List.of(), "HIDDEN", List.of()),
                List.of(),
                agentStateAssembler.emptySnapshot(),
                null,
                Instant.now()
        );
    }

    public WebWorkbenchDtos.WorkbenchSnapshot createSession(OrchestrationMode mode) {
        var session = tastingSessionService.createSession(mode == null ? OrchestrationMode.EXPLICIT_WORKFLOW : mode);
        return snapshot(session.id(), null, List.of());
    }

    public WebWorkbenchDtos.WorkbenchSnapshot submitMessage(String sessionId, String content, String modelMode) {
        recordUserMessage(sessionId, content);
        return completeAssistantTurn(sessionId, modelMode).snapshot();
    }

    public WebWorkbenchDtos.WorkbenchSnapshot submitMessage(String sessionId, String content) {
        return submitMessage(sessionId, content, null);
    }

    public WebWorkbenchDtos.WorkbenchSnapshot snapshot(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return emptySnapshot();
        }
        return snapshot(sessionId, null, List.of(), null, lastModelResults.get(sessionId));
    }

    public WebWorkbenchDtos.WorkbenchSnapshot recoverableSnapshot(String sessionId, ApiError error) {
        if (sessionId == null || sessionId.isBlank()) {
            return emptySnapshotWithError(error);
        }
        return snapshot(sessionId, error, List.of(), null, lastModelResults.get(sessionId));
    }

    public WebWorkbenchDtos.WebConversationMessage recordUserMessage(String sessionId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw userFixable("EMPTY_MESSAGE", "请先输入今天这杯咖啡的体验，再提交。", content);
        }
        ConversationMessage message = tastingSessionService.recordUserMessage(sessionId, content.trim());
        return toWebMessage(message);
    }

    public AssistantTurnResult completeAssistantTurn(String sessionId, String modelMode) {
        TastingSessionApplicationService.WorkspaceSnapshot workspace = tastingSessionService.workspace(sessionId);
        ModelGateway.ModelResult modelResult = agentStateAssembler.completeModel(workspace, modelMode);
        if (modelResult == null) {
            lastModelResults.remove(sessionId);
        } else if (modelResult.recoverableError() == null) {
            TastingSessionApplicationService.FactUpdateApplicationResult applicationResult =
                    tastingSessionService.applyFactUpdates(sessionId, modelResult.factUpdates());
            if (!applicationResult.applied()) {
                modelResult = factUpdateRejectedResult(modelResult, applicationResult.errors(), sessionId);
            }
            lastModelResults.put(sessionId, modelResult);
        } else {
            lastModelResults.put(sessionId, modelResult);
        }
        String assistantContent = assistantChatContent(modelResult);
        WebWorkbenchDtos.WebConversationMessage assistantMessage = null;
        if (modelResult != null && modelResult.recoverableError() == null && !assistantContent.isBlank()) {
            assistantMessage = toWebMessage(tastingSessionService.recordAssistantMessage(sessionId, assistantContent));
        }
        WebWorkbenchDtos.WorkbenchSnapshot snapshot = snapshot(sessionId, null, List.of(), modelMode, modelResult);
        return new AssistantTurnResult(assistantContent, assistantMessage, snapshot);
    }

    private WebWorkbenchDtos.WorkbenchSnapshot emptySnapshotWithError(ApiError error) {
        WebWorkbenchDtos.WorkbenchSnapshot empty = emptySnapshot();
        return new WebWorkbenchDtos.WorkbenchSnapshot(
                empty.sessionId(),
                WebWorkbenchSession.Status.ERROR_RECOVERABLE,
                empty.heroQuestion(),
                empty.orchestrationMode(),
                empty.conversation(),
                empty.recordSummary(),
                empty.draftTabs(),
                empty.agentState(),
                error,
                Instant.now()
        );
    }

    public WebWorkbenchDtos.WorkbenchSnapshot clearSession(String sessionId, boolean confirmed) {
        if (sessionId == null || sessionId.isBlank()) {
            return emptySnapshot();
        }
        if (!confirmed) {
            throw userFixable("CLEAR_SESSION_NOT_CONFIRMED", "清空当前会话需要先确认。", "");
        }
        tastingSessionService.clearSession(sessionId);
        lastModelResults.remove(sessionId);
        WebWorkbenchDtos.WorkbenchSnapshot empty = emptySnapshot();
        return new WebWorkbenchDtos.WorkbenchSnapshot(
                empty.sessionId(),
                empty.status(),
                empty.heroQuestion(),
                empty.orchestrationMode(),
                empty.conversation(),
                empty.recordSummary(),
                empty.draftTabs(),
                agentStateAssembler.clearedSnapshot(),
                empty.lastError(),
                Instant.now()
        );
    }

    private WebWorkbenchDtos.WorkbenchSnapshot snapshot(String sessionId, ApiError lastError, List<String> explicitPendingQuestions) {
        return snapshot(sessionId, lastError, explicitPendingQuestions, null, lastModelResults.get(sessionId));
    }

    private WebWorkbenchDtos.WorkbenchSnapshot snapshot(String sessionId, ApiError lastError, List<String> explicitPendingQuestions, String modelMode) {
        return snapshot(sessionId, lastError, explicitPendingQuestions, modelMode, lastModelResults.get(sessionId));
    }

    private WebWorkbenchDtos.WorkbenchSnapshot snapshot(String sessionId, ApiError lastError, List<String> explicitPendingQuestions, String modelMode, ModelGateway.ModelResult modelResult) {
        TastingSessionApplicationService.WorkspaceSnapshot workspace = tastingSessionService.workspace(sessionId);
        List<WebWorkbenchDtos.WebConversationMessage> conversation = workspace.conversation().stream()
                .map(this::toWebMessage)
                .toList();
        WebWorkbenchDtos.AgentStateSnapshot agentState = modelResult == null
                ? agentStateAssembler.assemble(workspace, modelMode)
                : agentStateAssembler.assembleWithModelResult(workspace, modelMode, modelResult);
        List<WebWorkbenchDtos.DraftTab> draftTabs = modelOutputDraftTabs(agentState.modelOutput(), "由真实模型生成，事实边界仍需检查。");
        List<String> pendingQuestions = agentState.modelOutput() != null && agentState.modelOutput().conversation() != null
                ? agentState.modelOutput().conversation().questions()
                : List.of();
        List<String> factBoundaryNotes = draftTabs.stream()
                .flatMap(draft -> draft.factBoundaryNotes().stream())
                .distinct()
                .toList();
        ApiError modelError = toApiError(agentState.modelOutput() == null ? null : agentState.modelOutput().recoverableError());
        ApiError effectiveError = lastError == null ? modelError : lastError;
        WebWorkbenchSession.Status status = effectiveError != null
                ? WebWorkbenchSession.Status.ERROR_RECOVERABLE
                : draftTabs.isEmpty()
                ? (conversation.isEmpty() ? WebWorkbenchSession.Status.SESSION_CREATED : WebWorkbenchSession.Status.WAITING_FOR_FACTS)
                : WebWorkbenchSession.Status.DRAFTS_READY;
        return new WebWorkbenchDtos.WorkbenchSnapshot(
                workspace.sessionId(),
                status,
                workspace.heroQuestion(),
                workspace.orchestrationMode(),
                conversation,
                new WebWorkbenchDtos.RecordSummary(
                        workspace.confirmedFacts().stream().map(com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem::value).toList(),
                        pendingQuestions,
                        List.of(),
                        draftTabs.isEmpty() ? "HIDDEN" : "VISIBLE",
                        factBoundaryNotes
                ),
                draftTabs,
                agentState,
                effectiveError,
                Instant.now()
        );
    }

    private WebWorkbenchDtos.WebConversationMessage toWebMessage(ConversationMessage message) {
        return new WebWorkbenchDtos.WebConversationMessage(
                message.id(),
                message.role().name(),
                message.content(),
                message.sourceType(),
                message.createdAt()
        );
    }

    private WebWorkbenchDtos.DraftTab toDraftTab(DraftCopy draft) {
        return new WebWorkbenchDtos.DraftTab(
                draft.id(),
                draft.style().name(),
                draft.title(),
                draft.body(),
                draft.tags(),
                draft.factBoundaryNotes(),
                draft.reviewWarnings()
        );
    }

    private List<WebWorkbenchDtos.DraftTab> modelOutputDraftTabs(WebWorkbenchDtos.ModelOutputSnapshot modelOutput, String defaultFactBoundaryNote) {
        if (modelOutput == null
                || modelOutput.messageType() != ModelMessageType.POST
                || modelOutput.post() == null
                || modelOutput.post().variants().isEmpty()
                || modelOutput.recoverableError() != null) {
            return List.of();
        }
        return modelOutput.post().variants().stream()
                .map(variant -> toDraftTab(variant, defaultFactBoundaryNote))
                .toList();
    }

    private WebWorkbenchDtos.DraftTab toDraftTab(CopyVariant variant, String defaultFactBoundaryNote) {
        List<String> factBoundaryNotes = variant.validationWarnings().isEmpty()
                ? List.of(defaultFactBoundaryNote)
                : variant.validationWarnings();
        return new WebWorkbenchDtos.DraftTab(
                "model-" + variant.style().name().toLowerCase(),
                variant.style().name(),
                variant.title(),
                variant.body(),
                variant.tags(),
                factBoundaryNotes,
                variant.warnings()
        );
    }

    /**
     * 将事实增量校验失败安全降级为可恢复模型错误，阻断草稿和任何后续高影响动作。
     *
     * @param original 原始模型结果
     * @param validationErrors 已脱离用户敏感原文的协议错误摘要
     * @param sessionId 当前会话 ID
     * @return 保留请求预览但不暴露未应用业务输出的失败结果
     */
    private ModelGateway.ModelResult factUpdateRejectedResult(ModelGateway.ModelResult original, List<String> validationErrors, String sessionId) {
        com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError error =
                com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError.of(
                        com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError.Code.MODEL_FORMAT_INVALID,
                        "模型事实状态更新未通过安全校验，当前会话状态未改变，请重试。",
                        sessionId,
                        "RETRY"
                );
        return new ModelGateway.ModelResult(
                original.mode(), "ERROR", original.modelName(), "事实状态更新已拒绝",
                "模型输出未通过证据或状态流转校验，未写入会话。", "", null, "", null, null,
                List.of(), List.of("事实增量校验失败，共 " + validationErrors.size() + " 项"), List.of(),
                original.requestPreview(), original.responsePreview(), error, Instant.now()
        );
    }

    private CoffeeAgentException userFixable(String code, String message, String preservedInput) {
        ApiError error = ApiError.of(code, ErrorCategory.USER_FIXABLE, message, true, "KEEP_TYPING", "RETRY")
                .withDetails(Map.of("preservedInput", preservedInput == null ? "" : preservedInput));
        return new CoffeeAgentException(error);
    }

    private ApiError toApiError(com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError error) {
        if (error == null) {
            return null;
        }
        return ApiError.of(error.code().name(), ErrorCategory.RETRYABLE, error.message(), error.recoverable(), error.nextActions().toArray(String[]::new))
                .withDetails(Map.of(
                        "modelMode", error.retryableMode(),
                        "preservedSessionId", error.preservedSessionId()
                ));
    }

    private String assistantChatContent(ModelGateway.ModelResult modelResult) {
        if (modelResult == null) {
            return "";
        }
        if (modelResult.recoverableError() != null) {
            return modelResult.recoverableError().message();
        }
        List<String> parts = new ArrayList<>();
        if (modelResult.talk() != null && !modelResult.talk().isBlank()) {
            parts.add(modelResult.talk().trim());
        }
        return String.join("\n\n", parts);
    }

    public record AssistantTurnResult(
            String assistantContent,
            WebWorkbenchDtos.WebConversationMessage assistantMessage,
            WebWorkbenchDtos.WorkbenchSnapshot snapshot
    ) {
    }
}
