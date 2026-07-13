package com.minyuwei.xhs.coffeeagent.tasting.application;

import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftCopy;
import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory;
import com.minyuwei.xhs.coffeeagent.tasting.domain.ConversationMessage;
import com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateChange;
import com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSessionRepository;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;

import java.util.List;

public class TastingSessionApplicationService {
    private final TastingSessionRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final FactUpdateValidator factUpdateValidator;
    private final FactUpdateApplier factUpdateApplier;

    /**
     * 创建会话应用服务并使用默认的确定性事实增量校验与应用组件。
     *
     * @param repository 会话持久化端口
     * @param currentUserProvider 当前本地用户提供器
     */
    public TastingSessionApplicationService(TastingSessionRepository repository, CurrentUserProvider currentUserProvider) {
        this(repository, currentUserProvider, new FactUpdateValidator(), new FactUpdateApplier());
    }

    /**
     * 创建可注入事实状态组件的会话应用服务，便于边界测试和替换持久化实现。
     *
     * @param repository 会话持久化端口
     * @param currentUserProvider 当前本地用户提供器
     * @param factUpdateValidator 模型事实增量的确定性校验器
     * @param factUpdateApplier 已校验增量的状态应用器
     */
    public TastingSessionApplicationService(TastingSessionRepository repository, CurrentUserProvider currentUserProvider,
                                            FactUpdateValidator factUpdateValidator, FactUpdateApplier factUpdateApplier) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.factUpdateValidator = factUpdateValidator;
        this.factUpdateApplier = factUpdateApplier;
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

    /**
     * 原子校验并应用主模型返回的事实状态增量；整批非法时不修改会话。
     *
     * @param sessionId 目标会话 ID
     * @param updates 主模型本轮返回的状态增量
     * @return 应用结果，包含是否成功和可安全写入 Trace 的协议错误摘要
     */
    public FactUpdateApplicationResult applyFactUpdates(String sessionId, List<FactUpdate> updates) {
        TastingSession session = find(sessionId);
        List<String> errors = factUpdateValidator.validate(session, updates);
        if (!errors.isEmpty()) {
            return new FactUpdateApplicationResult(false, errors);
        }
        factUpdateApplier.apply(session, updates);
        repository.save(session);
        return new FactUpdateApplicationResult(true, List.of());
    }

    /**
     * 读取模型调用和工作台展示所需的真实会话状态快照。
     *
     * @param sessionId 会话 ID
     * @return 对话、草稿、事实状态及变更历史快照
     */
    public WorkspaceSnapshot workspace(String sessionId) {
        TastingSession session = find(sessionId);
        return new WorkspaceSnapshot(session.id(), "今天喝了什么咖啡？", session.orchestrationMode(), session.messages(), session.drafts(),
                session.confirmedFactItems(), session.pendingAssociationItems(), session.rejectedAssociationItems(), session.factStateChanges());
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

    public record FactUpdateApplicationResult(boolean applied, List<String> errors) {
        public FactUpdateApplicationResult {
            errors = errors == null ? List.of() : List.copyOf(errors);
        }
    }

    public record WorkspaceSnapshot(
            String sessionId,
            String heroQuestion,
            OrchestrationMode orchestrationMode,
            List<ConversationMessage> conversation,
            List<DraftCopy> draftTabs,
            List<FactStateItem> confirmedFacts,
            List<FactStateItem> pendingAssociations,
            List<FactStateItem> rejectedAssociations,
            List<FactStateChange> factStateChanges
    ) {
    }
}
