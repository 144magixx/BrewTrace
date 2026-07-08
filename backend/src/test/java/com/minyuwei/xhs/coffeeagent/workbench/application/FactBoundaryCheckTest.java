package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.shared.domain.ConfirmationStatus;
import com.minyuwei.xhs.coffeeagent.workbench.api.WebWorkbenchDtos;
import com.minyuwei.xhs.coffeeagent.workbench.domain.AgentStateModels;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FactBoundaryCheckTest {
    @Test
    void coversConfirmedPendingUnsupportedAndConflictResults() {
        FactBoundaryChecker checker = new FactBoundaryChecker();

        var checks = checker.check(
                new WebWorkbenchDtos.ModelOutputSnapshot("REAL_MODEL", "真实模型输出", "由真实模型生成，事实边界仍需检查。", Instant.now()),
                List.of(new WebWorkbenchDtos.ConfirmedFact("fact-1", "FLAVOR", "用户确认风味：柑橘", "ctx-1", ConfirmationStatus.CONFIRMED, AgentStateModels.SendStatus.WILL_SEND)),
                List.of(new WebWorkbenchDtos.PendingAssociation("assoc-1", "甜橙", "fact-1", "由柑橘感扩展", ConfirmationStatus.PENDING_CONFIRMATION, AgentStateModels.SendStatus.SEND_AFTER_CONFIRMATION)),
                List.of(new WebWorkbenchDtos.CandidateMemory("mem-1", "冲突记忆候选", "曾经记录偏好深烘坚果调，与当前清爽水洗埃塞表达冲突。", "真实长期数据库召回", "与当前已确认事实冲突", "CONFLICTING_PREFERENCE", "冲突风味关键词", "CONFLICT", AgentStateModels.SendStatus.EXCLUDED))
        );

        assertTrue(checks.stream().anyMatch(check -> check.basisType() == AgentStateModels.BasisType.USER_CONFIRMED));
        assertTrue(checks.stream().anyMatch(check -> check.basisType() == AgentStateModels.BasisType.PENDING_ASSOCIATION));
        assertTrue(checks.stream().anyMatch(check -> check.basisType() == AgentStateModels.BasisType.UNSUPPORTED));
        assertTrue(checks.stream().anyMatch(check -> check.basisType() == AgentStateModels.BasisType.CONFLICT));
    }
}
