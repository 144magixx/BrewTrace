package com.minyuwei.xhs.coffeeagent.tasting.application;

import com.minyuwei.xhs.coffeeagent.agent.application.AgentOrchestrator;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftCopy;
import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory;
import com.minyuwei.xhs.coffeeagent.tasting.domain.ConversationMessage;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSessionRepository;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;

import java.util.List;

public class TastingSessionApplicationService {
    private final TastingSessionRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final AgentOrchestrator orchestrator;

    public TastingSessionApplicationService(TastingSessionRepository repository, CurrentUserProvider currentUserProvider, AgentOrchestrator orchestrator) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.orchestrator = orchestrator;
    }

    public TastingSession createSession(OrchestrationMode mode) {
        return repository.save(TastingSession.create(currentUserProvider.currentUserId(), mode));
    }

    public MessageResult submitMessage(String sessionId, String content) {
        TastingSession session = find(sessionId);
        session.addUserMessage(content);
        AgentOrchestrator.TurnResult turn = orchestrator.handleUserTurn(session);
        session.addAssistantMessage(turn.assistantMessage());
        if (!turn.drafts().isEmpty()) {
            session.addDrafts(turn.drafts());
        }
        repository.save(session);
        return new MessageResult(turn.assistantMessage(), turn.pendingQuestions(), turn.drafts(), "trace-" + session.id());
    }

    public List<DraftCopy> generateDrafts(String sessionId) {
        TastingSession session = find(sessionId);
        List<DraftCopy> drafts = orchestrator.generateDrafts(session);
        session.addDrafts(drafts);
        repository.save(session);
        return drafts;
    }

    public WorkspaceSnapshot workspace(String sessionId) {
        TastingSession session = find(sessionId);
        return new WorkspaceSnapshot(session.id(), "今天喝了什么咖啡？", session.orchestrationMode(), session.messages(), session.drafts());
    }

    private TastingSession find(String sessionId) {
        return repository.findById(sessionId).orElseThrow(() -> new CoffeeAgentException(ApiError.of(
                "SESSION_NOT_FOUND",
                ErrorCategory.USER_FIXABLE,
                "品鉴会话不存在。",
                true,
                "CREATE_SESSION"
        )));
    }

    public record MessageResult(String assistantMessage, List<String> pendingQuestions, List<DraftCopy> drafts, String traceId) {
    }

    public record WorkspaceSnapshot(
            String sessionId,
            String heroQuestion,
            OrchestrationMode orchestrationMode,
            List<ConversationMessage> conversation,
            List<DraftCopy> draftTabs
    ) {
    }
}
