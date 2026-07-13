package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptComposer;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.shared.config.ModelProperties;
import com.minyuwei.xhs.coffeeagent.shared.domain.ConfirmationStatus;
import com.minyuwei.xhs.coffeeagent.shared.domain.SourceType;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.ConversationMessage;
import com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem;
import com.minyuwei.xhs.coffeeagent.workbench.api.WebWorkbenchDtos;
import com.minyuwei.xhs.coffeeagent.workbench.domain.AgentStateModels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AgentStateAssembler {
    private final FactBoundaryChecker factBoundaryChecker;
    private final ModelContextPackageAssembler contextPackageAssembler;
    private final ModelGateway modelGateway;
    private final String configuredMode;
    private final String baseUrlLabel;
    private final String modelName;

    @Autowired
    public AgentStateAssembler(
            FactBoundaryChecker factBoundaryChecker,
            ModelContextPackageAssembler contextPackageAssembler,
            ModelGateway modelGateway,
            ModelProperties modelProperties
    ) {
        this.factBoundaryChecker = factBoundaryChecker;
        this.contextPackageAssembler = contextPackageAssembler;
        this.modelGateway = modelGateway;
        this.configuredMode = modelProperties.mode();
        this.baseUrlLabel = modelProperties.baseUrl();
        this.modelName = modelProperties.textModel();
    }

    public AgentStateAssembler(FactBoundaryChecker factBoundaryChecker) {
        this.factBoundaryChecker = factBoundaryChecker;
        this.contextPackageAssembler = new ModelContextPackageAssembler();
        PromptTemplateLoader promptTemplateLoader = new PromptTemplateLoader();
        this.modelGateway = new com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiModelGateway(new com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiResponsesLlmClient(), new com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiResponsesRequestFactory(new PromptComposer(promptTemplateLoader), promptTemplateLoader), new com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiResponsesParser(), "https://saturday.sankuai.com/v1", "gpt-5.5", 120, "");
        this.configuredMode = ModelMode.OPENAI_GPT55.code();
        this.baseUrlLabel = "https://saturday.sankuai.com/v1";
        this.modelName = "gpt-5.5";
    }

    public AgentStateAssembler(FactBoundaryChecker factBoundaryChecker, ModelGateway modelGateway) {
        this.factBoundaryChecker = factBoundaryChecker;
        this.contextPackageAssembler = new ModelContextPackageAssembler();
        this.modelGateway = modelGateway;
        this.configuredMode = ModelMode.OPENAI_GPT55.code();
        this.baseUrlLabel = "https://saturday.sankuai.com/v1";
        this.modelName = "gpt-5.5";
    }

    public WebWorkbenchDtos.AgentStateSnapshot emptySnapshot() {
        Instant now = Instant.now();
        return new WebWorkbenchDtos.AgentStateSnapshot(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new WebWorkbenchDtos.ContextPreview(List.of(
                        new WebWorkbenchDtos.ContextPreviewSection("CURRENT_SESSION", "当前会话", List.of()),
                        new WebWorkbenchDtos.ContextPreviewSection("CONFIRMED_FACTS", "已确认事实", List.of()),
                        new WebWorkbenchDtos.ContextPreviewSection("PENDING_ASSOCIATIONS", "待确认联想", List.of()),
                        new WebWorkbenchDtos.ContextPreviewSection("CANDIDATE_MEMORIES", "候选记忆", List.of())
                ), 0, 0, "当前没有可发送上下文。输入后将调用 GPT-5.5。", null, null),
                modeSnapshot(ModelMode.OPENAI_GPT55),
                null,
                List.of(),
                capabilityBoundary(false),
                sessionControl(false, "IDLE"),
                now
        );
    }

    public WebWorkbenchDtos.AgentStateSnapshot assemble(TastingSessionApplicationService.WorkspaceSnapshot workspace) {
        return assemble(workspace, null);
    }

    public WebWorkbenchDtos.AgentStateSnapshot assemble(TastingSessionApplicationService.WorkspaceSnapshot workspace, String requestedMode) {
        return assembleWithModelResult(workspace, requestedMode, completeModel(workspace, requestedMode));
    }

    public ModelGateway.ModelResult completeModel(TastingSessionApplicationService.WorkspaceSnapshot workspace, String requestedMode) {
        List<WebWorkbenchDtos.ContextItem> contextItems = contextItems(workspace.conversation());
        List<WebWorkbenchDtos.ConfirmedFact> confirmedFacts = confirmedFacts(workspace.confirmedFacts());
        ModelMode mode = resolveMode(requestedMode);
        List<WebWorkbenchDtos.PendingAssociation> pendingAssociations = pendingAssociations(workspace.pendingAssociations());
        List<WebWorkbenchDtos.CandidateMemory> candidateMemories = List.of();
        return contextItems.isEmpty()
                ? null
                : gateway(mode).complete(contextPackageAssembler.assemble(workspace.sessionId(), mode, contextItems, confirmedFacts, pendingAssociations, candidateMemories));
    }

    public WebWorkbenchDtos.AgentStateSnapshot assembleWithModelResult(TastingSessionApplicationService.WorkspaceSnapshot workspace, String requestedMode, ModelGateway.ModelResult modelResult) {
        List<WebWorkbenchDtos.ContextItem> contextItems = contextItems(workspace.conversation());
        List<WebWorkbenchDtos.ConfirmedFact> confirmedFacts = confirmedFacts(workspace.confirmedFacts());
        ModelMode mode = resolveMode(requestedMode);
        List<WebWorkbenchDtos.PendingAssociation> pendingAssociations = pendingAssociations(workspace.pendingAssociations());
        List<WebWorkbenchDtos.CandidateMemory> candidateMemories = List.of();
        WebWorkbenchDtos.ContextPreview contextPreview = contextPreview(contextItems, confirmedFacts, pendingAssociations, candidateMemories, modelResult);
        WebWorkbenchDtos.ModelOutputSnapshot modelOutput = modelOutput(modelResult);
        List<WebWorkbenchDtos.FactBoundaryCheckResult> boundaryChecks = factBoundaryChecker.check(modelOutput, confirmedFacts, pendingAssociations, candidateMemories);
        boolean realModelConnected = modelResult != null && "REAL_MODEL".equals(modelResult.outputType());
        return new WebWorkbenchDtos.AgentStateSnapshot(
                statusCards(contextItems, confirmedFacts, pendingAssociations, candidateMemories, contextPreview, modelOutput, boundaryChecks, mode),
                contextItems,
                confirmedFacts,
                pendingAssociations,
                candidateMemories,
                contextPreview,
                modeSnapshot(modelResult == null ? mode : modelResult.mode()),
                modelOutput,
                boundaryChecks,
                capabilityBoundary(realModelConnected),
                sessionControl(!workspace.conversation().isEmpty() || !workspace.draftTabs().isEmpty(), "IDLE"),
                Instant.now()
        );
    }

    public WebWorkbenchDtos.AgentStateSnapshot clearedSnapshot() {
        WebWorkbenchDtos.AgentStateSnapshot empty = emptySnapshot();
        return new WebWorkbenchDtos.AgentStateSnapshot(
                empty.statusCards(),
                empty.contextItems(),
                empty.confirmedFacts(),
                empty.pendingAssociations(),
                empty.candidateMemories(),
                empty.contextPreview(),
                empty.modelMode(),
                empty.modelOutput(),
                empty.factBoundaryChecks(),
                empty.capabilityBoundary(),
                sessionControl(false, "CLEARED"),
                empty.updatedAt()
        );
    }

    /**
     * 将完整会话历史映射为模型上下文候选，并保留用户与助手角色顺序。
     * 助手消息可用于理解下一轮回答，但仍保持模型来源和待确认边界，不能充当用户事实证据。
     *
     * @param conversation 当前会话中按时间顺序保存的用户与助手消息
     * @return 全部允许发送给模型的会话上下文项
     */
    private List<WebWorkbenchDtos.ContextItem> contextItems(List<ConversationMessage> conversation) {
        return conversation.stream()
                .map(message -> new WebWorkbenchDtos.ContextItem(
                        "context-" + message.id(),
                        message.role().name(),
                        message.content(),
                        message.sourceType(),
                        message.role() == ConversationMessage.Role.USER ? ConfirmationStatus.CONFIRMED : ConfirmationStatus.PENDING_CONFIRMATION,
                        AgentStateModels.SendStatus.WILL_SEND,
                        message.id(),
                        message.createdAt()
                ))
                .toList();
    }

    /**
     * 将领域层已确认事实映射为工作台 DTO，不根据事实文本猜测分类或语义。
     *
     * @param facts 会话中已通过校验的确认事实状态
     * @return 保留证据和边界的确认事实 DTO
     */
    private List<WebWorkbenchDtos.ConfirmedFact> confirmedFacts(List<FactStateItem> facts) {
        return facts.stream().map(fact -> new WebWorkbenchDtos.ConfirmedFact(
                fact.id(),
                "TASTING_FACT",
                fact.value(),
                "context-" + fact.sourceMessageId(),
                fact.sourceMessageId(),
                fact.sourceQuote(),
                fact.reason(),
                fact.boundary(),
                ConfirmationStatus.CONFIRMED,
                AgentStateModels.SendStatus.WILL_SEND
        )).toList();
    }

    /**
     * 将领域层待确认联想映射为工作台 DTO，保持其不可作为确认事实发送的边界。
     *
     * @param associations 会话中当前待确认的联想状态
     * @return 保留证据和模型理由的待确认联想 DTO
     */
    private List<WebWorkbenchDtos.PendingAssociation> pendingAssociations(List<FactStateItem> associations) {
        return associations.stream().map(association -> new WebWorkbenchDtos.PendingAssociation(
                association.id(),
                association.value(),
                null,
                association.sourceMessageId(),
                association.sourceQuote(),
                association.reason(),
                association.boundary(),
                ConfirmationStatus.PENDING_CONFIRMATION,
                AgentStateModels.SendStatus.SEND_AFTER_CONFIRMATION
        )).toList();
    }

    private WebWorkbenchDtos.ContextPreview contextPreview(
            List<WebWorkbenchDtos.ContextItem> contextItems,
            List<WebWorkbenchDtos.ConfirmedFact> confirmedFacts,
            List<WebWorkbenchDtos.PendingAssociation> pendingAssociations,
            List<WebWorkbenchDtos.CandidateMemory> candidateMemories,
            ModelGateway.ModelResult modelResult
    ) {
        List<WebWorkbenchDtos.ContextPreviewSection> sections = List.of(
                new WebWorkbenchDtos.ContextPreviewSection("CURRENT_SESSION", "当前会话", contextItems.stream()
                        .map(item -> new WebWorkbenchDtos.ContextPreviewItem(item.content(), sourceLabel(item.sourceType()), item.sendStatus(), exclusionReason(item.sendStatus())))
                        .toList()),
                new WebWorkbenchDtos.ContextPreviewSection("CONFIRMED_FACTS", "已确认事实", confirmedFacts.stream()
                        .map(fact -> new WebWorkbenchDtos.ContextPreviewItem(fact.value(), "来自用户消息", fact.sendStatus(), null))
                        .toList()),
                new WebWorkbenchDtos.ContextPreviewSection("PENDING_ASSOCIATIONS", "待确认联想", pendingAssociations.stream()
                        .map(association -> new WebWorkbenchDtos.ContextPreviewItem(association.value(), association.reason(), association.sendStatus(), exclusionReason(association.sendStatus())))
                        .toList()),
                new WebWorkbenchDtos.ContextPreviewSection("CANDIDATE_MEMORIES", "候选记忆", candidateMemories.stream()
                        .map(memory -> new WebWorkbenchDtos.ContextPreviewItem(memory.title() + "：" + memory.content(), memory.sourceBoundary(), memory.sendStatus(), exclusionReason(memory.sendStatus())))
                        .toList())
        );
        List<AgentStateModels.SendStatus> statuses = sections.stream()
                .flatMap(section -> section.items().stream())
                .map(WebWorkbenchDtos.ContextPreviewItem::sendStatus)
                .toList();
        return new WebWorkbenchDtos.ContextPreview(
                sections,
                (int) statuses.stream().filter(AgentStateModels.SendStatus.WILL_SEND::equals).count(),
                (int) statuses.stream().filter(AgentStateModels.SendStatus.EXCLUDED::equals).count(),
                modelResult == null ? "当前没有可发送上下文。输入后将调用 GPT-5.5。" : "真实模型模式开启，以下内容已组织为 GPT-5.5 请求。",
                modelResult == null ? null : modelResult.requestPreview(),
                modelResult == null ? null : modelResult.responsePreview()
        );
    }

    private List<WebWorkbenchDtos.AgentStatusCard> statusCards(
            List<WebWorkbenchDtos.ContextItem> contextItems,
            List<WebWorkbenchDtos.ConfirmedFact> confirmedFacts,
            List<WebWorkbenchDtos.PendingAssociation> pendingAssociations,
            List<WebWorkbenchDtos.CandidateMemory> candidateMemories,
            WebWorkbenchDtos.ContextPreview contextPreview,
            WebWorkbenchDtos.ModelOutputSnapshot modelOutput,
            List<WebWorkbenchDtos.FactBoundaryCheckResult> boundaryChecks,
            ModelMode mode
    ) {
        Instant now = Instant.now();
        List<WebWorkbenchDtos.AgentStatusCard> cards = new ArrayList<>();
        addCard(cards, "card-session-context", AgentStateModels.AgentCardType.SESSION_CONTEXT, "当前会话上下文", "当前会话项：" + contextItems.size(), "来自当前页面会话", AgentStateModels.SendStatus.WILL_SEND, AgentStateModels.RiskLevel.INFO, now);
        addCard(cards, "card-confirmed-fact", AgentStateModels.AgentCardType.CONFIRMED_FACT, "已确认事实", "已确认事实：" + confirmedFacts.size(), "来自用户消息", AgentStateModels.SendStatus.WILL_SEND, AgentStateModels.RiskLevel.NONE, now);
        addCard(cards, "card-pending-association", AgentStateModels.AgentCardType.PENDING_ASSOCIATION, "待确认联想", "待确认联想：" + pendingAssociations.size(), "由已确认事实扩展", AgentStateModels.SendStatus.SEND_AFTER_CONFIRMATION, AgentStateModels.RiskLevel.WARNING, now);
        addCard(cards, "card-candidate-memory", AgentStateModels.AgentCardType.CANDIDATE_MEMORY, "候选记忆", "候选记忆：" + candidateMemories.size(), "未接真实长期数据库", AgentStateModels.SendStatus.PAGE_ONLY, AgentStateModels.RiskLevel.INFO, now);
        addCard(cards, "card-context-preview", AgentStateModels.AgentCardType.CONTEXT_PREVIEW, "上下文预览", "将发送 " + contextPreview.willSendCount() + " 项，排除 " + contextPreview.excludedCount() + " 项", "真实模型请求预览", AgentStateModels.SendStatus.PAGE_ONLY, AgentStateModels.RiskLevel.INFO, now);
        if (modelOutput != null) {
            String routeSummary = modelOutput.messageType() == null
                    ? modelOutput.statusLabel()
                    : modelOutput.statusLabel() + " / " + modelOutput.messageType();
            addCard(cards, "card-model-output", AgentStateModels.AgentCardType.MODEL_OUTPUT, "模型输出", routeSummary, modelOutput.sourceBoundary(), AgentStateModels.SendStatus.PAGE_ONLY, "ERROR".equals(modelOutput.outputType()) ? AgentStateModels.RiskLevel.HIGH : AgentStateModels.RiskLevel.WARNING, now);
        }
        if (!boundaryChecks.isEmpty()) {
            AgentStateModels.RiskLevel risk = boundaryChecks.stream().anyMatch(check -> check.riskLevel() == AgentStateModels.RiskLevel.HIGH)
                    ? AgentStateModels.RiskLevel.HIGH
                    : AgentStateModels.RiskLevel.WARNING;
            addCard(cards, "card-fact-boundary", AgentStateModels.AgentCardType.FACT_BOUNDARY_CHECK, "事实边界检查", "检查结果：" + boundaryChecks.size() + " 项", "基于模型输出和当前事实", AgentStateModels.SendStatus.PAGE_ONLY, risk, now);
        }
        String capabilitySummary = "已启用真实文本模型，未接长期数据库和小红书";
        addCard(cards, "card-capability-boundary", AgentStateModels.AgentCardType.CAPABILITY_BOUNDARY, "能力边界", capabilitySummary, "本地可视化链路", AgentStateModels.SendStatus.PAGE_ONLY, AgentStateModels.RiskLevel.INFO, now);
        return cards;
    }

    private void addCard(
            List<WebWorkbenchDtos.AgentStatusCard> cards,
            String id,
            AgentStateModels.AgentCardType type,
            String title,
            String summary,
            String sourceLabel,
            AgentStateModels.SendStatus sendStatus,
            AgentStateModels.RiskLevel riskLevel,
            Instant createdAt
    ) {
        cards.add(new WebWorkbenchDtos.AgentStatusCard(id, type, title, summary, sourceLabel, sendStatus, riskLevel, createdAt));
    }

    private WebWorkbenchDtos.CapabilityBoundary capabilityBoundary(boolean realModelConnected) {
        return new WebWorkbenchDtos.CapabilityBoundary(
                realModelConnected,
                false,
                false,
                realModelConnected
                        ? "已启用真实文本模型；仍未执行小红书动作，未接真实长期记忆数据库。"
                        : "已配置真实文本模型；等待输入或模型返回，未执行小红书动作，未接真实长期记忆数据库。"
        );
    }

    private WebWorkbenchDtos.SessionControlAction sessionControl(boolean confirmationRequired, String resultStatus) {
        return new WebWorkbenchDtos.SessionControlAction(
                "CLEAR_CURRENT_SESSION",
                confirmationRequired,
                "清空当前会话可见状态和浏览器恢复状态，不删除长期记忆、历史归档或外部平台数据。",
                false,
                resultStatus
        );
    }

    private ModelMode resolveMode(String requestedMode) {
        return ModelMode.fromCode(requestedMode == null || requestedMode.isBlank() ? configuredMode : requestedMode);
    }

    private ModelGateway gateway(ModelMode mode) {
        return modelGateway;
    }

    private WebWorkbenchDtos.ModelOutputSnapshot modelOutput(ModelGateway.ModelResult result) {
        if (result == null) {
            return null;
        }
        return new WebWorkbenchDtos.ModelOutputSnapshot(
                result.outputType(),
                result.mode().code(),
                result.modelName(),
                result.statusLabel(),
                result.content(),
                result.sourceBoundary(),
                result.messageType(),
                result.talk(),
                result.post(),
                result.conversation(),
                result.factUpdates(),
                result.warnings(),
                result.variants(),
                result.requestPreview(),
                result.responsePreview(),
                result.recoverableError(),
                result.generatedAt()
        );
    }

    private WebWorkbenchDtos.ModelModeSnapshot modeSnapshot(ModelMode mode) {
        return new WebWorkbenchDtos.ModelModeSnapshot(
                mode.code(),
                mode.displayName(),
                modelName,
                baseUrlLabel,
                true,
                mode.requiresApiKey(),
                mode.displayName(),
                false
        );
    }

    private String sourceLabel(SourceType sourceType) {
        return switch (sourceType) {
            case USER_CONFIRMED -> "来自用户消息";
            case MODEL_SUGGESTED -> "来自助手追问";
            case EXTERNAL_REFERENCE -> "来自外部参考";
            case IMAGE_EXTRACTED -> "来自图片识别";
            case PENDING_CONFIRMATION -> "待用户确认";
        };
    }

    private String exclusionReason(AgentStateModels.SendStatus sendStatus) {
        return switch (sendStatus) {
            case WILL_SEND -> null;
            case PAGE_ONLY -> "仅页面观察，不会发送给模型";
            case SEND_AFTER_CONFIRMATION -> "用户确认后才可能发送";
            case EXCLUDED -> "存在冲突或无依据，已排除";
        };
    }
}
