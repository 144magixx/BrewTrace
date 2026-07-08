package com.minyuwei.xhs.coffeeagent.tasting.application;

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

    public TastingSessionApplicationService(TastingSessionRepository repository, CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    public TastingSession createSession(OrchestrationMode mode) {
        return repository.save(TastingSession.create(currentUserProvider.currentUserId(), mode));
    }

    public MessageResult submitMessage(String sessionId, String content) {
        TastingSession session = find(sessionId);
        session.addUserMessage(content);
        repository.save(session);
        return new MessageResult("已记录用户输入，将由 GPT-5.5 工作台链路生成草稿。", List.of(), List.of(), "trace-" + session.id());
    }

    public ConversationMessage recordUserMessage(String sessionId, String content) {
        TastingSession session = find(sessionId);
        ConversationMessage message = session.addUserMessage(content);
        repository.save(session);
        return message;
    }

    public ConversationMessage recordAssistantMessage(String sessionId, String content) {
        TastingSession session = find(sessionId);
        ConversationMessage message = session.addAssistantMessage(content);
        repository.save(session);
        return message;
    }

    public List<DraftCopy> generateDrafts(String sessionId) {
        find(sessionId);
        return List.of();
    }

    public WorkspaceSnapshot workspace(String sessionId) {
        TastingSession session = find(sessionId);
        return new WorkspaceSnapshot(session.id(), "今天喝了什么咖啡？", session.orchestrationMode(), session.messages(), session.drafts(), session.confirmedFacts());
    }

    public void clearSession(String sessionId) {
        find(sessionId);
        repository.deleteById(sessionId);
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
            List<DraftCopy> draftTabs,
            List<String> confirmedFacts
    ) {
    }
}
