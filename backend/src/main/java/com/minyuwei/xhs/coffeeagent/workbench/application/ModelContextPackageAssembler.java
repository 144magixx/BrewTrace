package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem;
import com.minyuwei.xhs.coffeeagent.workbench.api.WebWorkbenchDtos;
import com.minyuwei.xhs.coffeeagent.workbench.domain.AgentStateModels;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ModelContextPackageAssembler {
    private static final String FACT_BOUNDARY_CONSTRAINTS = "prompts/agent/model-context-constraints-v1.json";

    private final PromptTemplateLoader promptTemplateLoader;

    public ModelContextPackageAssembler() {
        this(new PromptTemplateLoader());
    }

    @Autowired
    public ModelContextPackageAssembler(PromptTemplateLoader promptTemplateLoader) {
        this.promptTemplateLoader = promptTemplateLoader;
    }

    public ModelContextPackage assemble(
            String sessionId,
            ModelMode mode,
            List<WebWorkbenchDtos.ContextItem> contextItems,
            List<WebWorkbenchDtos.ConfirmedFact> confirmedFacts,
            List<WebWorkbenchDtos.PendingAssociation> pendingAssociations,
            List<WebWorkbenchDtos.CandidateMemory> candidateMemories
    ) {
        List<ModelContextPackage.ContextEntry> excluded = new ArrayList<>();
        excluded.addAll(contextItems.stream().filter(item -> item.sendStatus() == AgentStateModels.SendStatus.EXCLUDED).map(this::entry).toList());
        excluded.addAll(candidateMemories.stream().filter(memory -> memory.sendStatus() == AgentStateModels.SendStatus.EXCLUDED).map(this::entry).toList());
        return new ModelContextPackage(
                sessionId,
                mode,
                contextItems.stream()
                        .filter(item -> item.sendStatus() == AgentStateModels.SendStatus.WILL_SEND)
                        .map(this::entry)
                        .toList(),
                confirmedFacts.stream().map(this::entry).toList(),
                pendingAssociations.stream().map(this::entry).toList(),
                candidateMemories.stream().map(this::entry).toList(),
                excluded,
                promptConstraints(),
                Instant.now()
        );
    }

    private List<String> promptConstraints() {
        List<String> constraints = new ArrayList<>();
        promptTemplateLoader.loadJson(FACT_BOUNDARY_CONSTRAINTS).forEach(node -> constraints.add(node.asText()));
        return List.copyOf(constraints);
    }

    /**
     * 将页面会话项转换为模型上下文，保留消息角色以支持多轮指代解析。
     * 来源标签继续承担事实可信度边界，助手角色不会因此升级为用户确认事实。
     *
     * @param item 用户或助手产生的会话上下文项
     * @return 包含稳定消息 ID、原文和角色的模型上下文条目
     */
    private ModelContextPackage.ContextEntry entry(WebWorkbenchDtos.ContextItem item) {
        return new ModelContextPackage.ContextEntry(item.id(), item.content(), item.sourceType().name(), item.sendStatus().name(),
                exclusionReason(item.sendStatus()), item.sourceMessageId(), item.content(), null, null, item.role());
    }

    private ModelContextPackage.ContextEntry entry(WebWorkbenchDtos.ConfirmedFact fact) {
        return new ModelContextPackage.ContextEntry(fact.id(), fact.value(), fact.boundary().name(), fact.sendStatus().name(),
                exclusionReason(fact.sendStatus()), fact.sourceMessageId(), fact.sourceQuote(), fact.reason(), FactStateItem.Status.CONFIRMED.name());
    }

    private ModelContextPackage.ContextEntry entry(WebWorkbenchDtos.PendingAssociation association) {
        return new ModelContextPackage.ContextEntry(association.id(), association.value(), association.boundary().name(), association.sendStatus().name(),
                exclusionReason(association.sendStatus()), association.sourceMessageId(), association.sourceQuote(), association.reason(), FactStateItem.Status.PENDING.name());
    }

    private ModelContextPackage.ContextEntry entry(WebWorkbenchDtos.CandidateMemory memory) {
        String content = memory.title() + "：" + memory.content();
        return new ModelContextPackage.ContextEntry(memory.id(), content, memory.sourceBoundary(), memory.sendStatus().name(),
                exclusionReason(memory.sendStatus()), null, null, memory.reason(), null);
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
