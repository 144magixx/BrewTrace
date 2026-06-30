package com.minyuwei.xhs.coffeeagent.workbench.application;

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
import java.util.List;
import java.util.Map;

@Service
public class WebWorkbenchService {
    private static final String HERO_QUESTION = "今天喝了什么咖啡？";
    private final TastingSessionApplicationService tastingSessionService;

    public WebWorkbenchService(TastingSessionApplicationService tastingSessionService) {
        this.tastingSessionService = tastingSessionService;
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
                null,
                Instant.now()
        );
    }

    public WebWorkbenchDtos.WorkbenchSnapshot createSession(OrchestrationMode mode) {
        var session = tastingSessionService.createSession(mode == null ? OrchestrationMode.EXPLICIT_WORKFLOW : mode);
        return snapshot(session.id(), null, List.of());
    }

    public WebWorkbenchDtos.WorkbenchSnapshot submitMessage(String sessionId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw userFixable("EMPTY_MESSAGE", "请先输入今天这杯咖啡的体验，再提交。", content);
        }
        if (content.trim().length() < 4) {
            throw userFixable("MESSAGE_TOO_SHORT", "这段描述太短了，请补充至少一种咖啡事实或感受。", content);
        }
        TastingSessionApplicationService.MessageResult result = tastingSessionService.submitMessage(sessionId, content.trim());
        return snapshot(sessionId, null, result.pendingQuestions());
    }

    public WebWorkbenchDtos.WorkbenchSnapshot snapshot(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return emptySnapshot();
        }
        return snapshot(sessionId, null, List.of());
    }

    public WebWorkbenchDtos.WorkbenchSnapshot recoverableSnapshot(String sessionId, ApiError error) {
        if (sessionId == null || sessionId.isBlank()) {
            return emptySnapshotWithError(error);
        }
        return snapshot(sessionId, error, List.of());
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
                error,
                Instant.now()
        );
    }

    private WebWorkbenchDtos.WorkbenchSnapshot snapshot(String sessionId, ApiError lastError, List<String> explicitPendingQuestions) {
        TastingSessionApplicationService.WorkspaceSnapshot workspace = tastingSessionService.workspace(sessionId);
        List<WebWorkbenchDtos.WebConversationMessage> conversation = workspace.conversation().stream()
                .map(this::toWebMessage)
                .toList();
        List<WebWorkbenchDtos.DraftTab> draftTabs = workspace.draftTabs().stream().map(this::toDraftTab).toList();
        List<String> pendingQuestions = explicitPendingQuestions.isEmpty() && draftTabs.isEmpty() && !conversation.isEmpty()
                ? List.of("豆名或烘焙商是什么？", "水温、粉水比或冲煮参数是多少？", "你想先看克制版、夸张版还是锐评版？")
                : explicitPendingQuestions;
        List<String> factBoundaryNotes = draftTabs.stream()
                .flatMap(draft -> draft.factBoundaryNotes().stream())
                .distinct()
                .toList();
        WebWorkbenchSession.Status status = lastError != null
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
                        workspace.confirmedFacts(),
                        pendingQuestions,
                        List.of("甜橙", "青柠", "葡萄柚"),
                        draftTabs.isEmpty() ? "HIDDEN" : "VISIBLE",
                        factBoundaryNotes
                ),
                draftTabs,
                lastError,
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

    private CoffeeAgentException userFixable(String code, String message, String preservedInput) {
        ApiError error = ApiError.of(code, ErrorCategory.USER_FIXABLE, message, true, "KEEP_TYPING", "RETRY")
                .withDetails(Map.of("preservedInput", preservedInput == null ? "" : preservedInput));
        return new CoffeeAgentException(error);
    }
}
