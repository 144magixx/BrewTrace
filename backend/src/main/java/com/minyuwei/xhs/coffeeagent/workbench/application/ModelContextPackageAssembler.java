package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.workbench.api.WebWorkbenchDtos;
import com.minyuwei.xhs.coffeeagent.workbench.domain.AgentStateModels;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ModelContextPackageAssembler {
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
                List.of(
                        "已确认事实可以进入文案依据。",
                        "待确认联想只能标为待确认，不能写成用户事实。",
                        "候选记忆仅来自真实长期记忆召回；当前未接入时为空。",
                        "不得新增未提供的豆庄、海拔、处理法或用户偏好。"
                ),
                Instant.now()
        );
    }

    private ModelContextPackage.ContextEntry entry(WebWorkbenchDtos.ContextItem item) {
        return new ModelContextPackage.ContextEntry(item.id(), item.content(), item.sourceType().name(), item.sendStatus().name(), exclusionReason(item.sendStatus()));
    }

    private ModelContextPackage.ContextEntry entry(WebWorkbenchDtos.ConfirmedFact fact) {
        return new ModelContextPackage.ContextEntry(fact.id(), fact.value(), "来自用户消息", fact.sendStatus().name(), exclusionReason(fact.sendStatus()));
    }

    private ModelContextPackage.ContextEntry entry(WebWorkbenchDtos.PendingAssociation association) {
        return new ModelContextPackage.ContextEntry(association.id(), association.value(), association.reason(), association.sendStatus().name(), exclusionReason(association.sendStatus()));
    }

    private ModelContextPackage.ContextEntry entry(WebWorkbenchDtos.CandidateMemory memory) {
        String content = memory.title() + "：" + memory.content();
        return new ModelContextPackage.ContextEntry(memory.id(), content, memory.sourceBoundary(), memory.sendStatus().name(), exclusionReason(memory.sendStatus()));
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
