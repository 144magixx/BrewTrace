package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.workbench.api.WebWorkbenchDtos;
import com.minyuwei.xhs.coffeeagent.workbench.domain.AgentStateModels;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FactBoundaryChecker {
    public List<WebWorkbenchDtos.FactBoundaryCheckResult> check(
            WebWorkbenchDtos.ModelOutputSnapshot output,
            List<WebWorkbenchDtos.ConfirmedFact> confirmedFacts,
            List<WebWorkbenchDtos.PendingAssociation> pendingAssociations,
            List<WebWorkbenchDtos.CandidateMemory> candidateMemories
    ) {
        if (output == null) {
            return List.of();
        }
        List<WebWorkbenchDtos.FactBoundaryCheckResult> checks = new ArrayList<>();
        if (output.conversation() != null) {
            output.conversation().pendingConfirmations().forEach(item -> checks.add(new WebWorkbenchDtos.FactBoundaryCheckResult(
                    "check-conversation-pending-" + checks.size(),
                    item.expression(),
                    AgentStateModels.BasisType.PENDING_ASSOCIATION,
                    AgentStateModels.RiskLevel.WARNING,
                    item.sourceReference(),
                    "这是模型希望用户确认的信息，不会写入已确认事实或草稿。",
                    AgentStateModels.RecommendedAction.ASK_USER_CONFIRMATION
            )));
        }
        if (output.post() != null) {
            output.post().variants().stream()
                    .flatMap(variant -> variant.pendingConfirmations().stream())
                    .forEach(item -> checks.add(new WebWorkbenchDtos.FactBoundaryCheckResult(
                            "check-post-pending-" + checks.size(),
                            item.expression(),
                            AgentStateModels.BasisType.PENDING_ASSOCIATION,
                            AgentStateModels.RiskLevel.WARNING,
                            item.sourceReference(),
                            "POST 中仍保留待确认项，前端应提示用户不要当作事实发布。",
                            AgentStateModels.RecommendedAction.ASK_USER_CONFIRMATION
                    )));
        }
        if (!confirmedFacts.isEmpty()) {
            checks.add(new WebWorkbenchDtos.FactBoundaryCheckResult(
                    "check-user-confirmed",
                    confirmedFacts.getFirst().value(),
                    AgentStateModels.BasisType.USER_CONFIRMED,
                    AgentStateModels.RiskLevel.NONE,
                    "来自用户消息",
                    "该表达有用户陈述依据，可以保留来源后进入最终记录。",
                    AgentStateModels.RecommendedAction.KEEP
            ));
        }
        if (!pendingAssociations.isEmpty()) {
            checks.add(new WebWorkbenchDtos.FactBoundaryCheckResult(
                    "check-pending-association",
                    pendingAssociations.getFirst().value() + "爆汁感很明显",
                    AgentStateModels.BasisType.PENDING_ASSOCIATION,
                    AgentStateModels.RiskLevel.WARNING,
                    pendingAssociations.getFirst().reason(),
                    "用户只确认了柑橘感，甜橙爆汁感需要进一步确认。",
                    AgentStateModels.RecommendedAction.ASK_USER_CONFIRMATION
            ));
            checks.add(new WebWorkbenchDtos.FactBoundaryCheckResult(
                    "check-model-inference",
                    "清爽明亮",
                    AgentStateModels.BasisType.MODEL_INFERENCE,
                    AgentStateModels.RiskLevel.INFO,
                    "由已确认风味推断",
                    "这是模型推断，不会展示为用户确认事实。",
                    AgentStateModels.RecommendedAction.KEEP
            ));
        }
        checks.add(new WebWorkbenchDtos.FactBoundaryCheckResult(
                "check-unsupported",
                "庄园海拔 2000 米",
                AgentStateModels.BasisType.UNSUPPORTED,
                AgentStateModels.RiskLevel.HIGH,
                "当前会话没有该豆袋或外部资料来源",
                "该表达没有用户依据或外部来源，本切片不得写成事实。",
                AgentStateModels.RecommendedAction.EXCLUDE_FROM_FINAL_RECORD
        ));
        candidateMemories.stream()
                .filter(memory -> "CONFLICT".equals(memory.conflictStatus()))
                .findFirst()
                .ifPresent(memory -> checks.add(new WebWorkbenchDtos.FactBoundaryCheckResult(
                        "check-conflict",
                        memory.content(),
                        AgentStateModels.BasisType.CONFLICT,
                        AgentStateModels.RiskLevel.HIGH,
                        memory.sourceBoundary(),
                        "候选记忆与当前会话事实存在冲突，默认排除且不发送。",
                        AgentStateModels.RecommendedAction.REWRITE
                )));
        return checks;
    }
}
