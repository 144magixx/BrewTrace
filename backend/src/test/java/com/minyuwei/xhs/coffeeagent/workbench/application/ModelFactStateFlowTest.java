package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMessageType;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.infrastructure.TastingSessionRepositoryAdapter;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelFactStateFlowTest {
    @Test
    void persistsValidatedUpdatesAndSendsThemInNextContextPackage() {
        TastingSessionApplicationService sessions = new TastingSessionApplicationService(
                new TastingSessionRepositoryAdapter(), new CurrentUserProvider());
        CapturingGateway gateway = new CapturingGateway();
        AgentStateAssembler assembler = new AgentStateAssembler(new FactBoundaryChecker(), gateway);
        WebWorkbenchService workbench = new WebWorkbenchService(sessions, assembler);
        String sessionId = workbench.createSession(OrchestrationMode.EXPLICIT_WORKFLOW).sessionId();

        workbench.submitMessage(sessionId, "我明确喝到柑橘感，但可能有一点红茶。", null);
        assertEquals(List.of("风味：柑橘"), workbench.snapshot(sessionId).recordSummary().confirmedFacts());
        assertEquals(1, workbench.snapshot(sessionId).agentState().pendingAssociations().size());

        workbench.submitMessage(sessionId, "我再补充一点。", null);
        assertNotNull(gateway.lastContext);
        assertEquals(1, gateway.lastContext.confirmedFacts().size());
        assertEquals("柑橘感", gateway.lastContext.confirmedFacts().getFirst().sourceQuote());
        assertEquals(1, gateway.lastContext.pendingAssociations().size());
        assertTrue(gateway.lastContext.currentSession().stream().allMatch(entry -> entry.sourceMessageId() != null));
    }

    private static final class CapturingGateway implements ModelGateway {
        private ModelContextPackage lastContext;
        private int calls;

        @Override
        public ModelResult complete(ModelContextPackage contextPackage) {
            lastContext = contextPackage;
            calls++;
            List<FactUpdate> updates = calls == 1 ? List.of(
                    new FactUpdate(FactUpdate.Action.ADD_CONFIRMED_FACT, FactUpdate.Boundary.USER_STATED,
                            "风味：柑橘", latestUserMessageId(contextPackage), "柑橘感", "用户明确陈述", null),
                    new FactUpdate(FactUpdate.Action.ADD_PENDING_ASSOCIATION, FactUpdate.Boundary.USER_UNCERTAIN,
                            "风味：红茶", latestUserMessageId(contextPackage), "可能有一点红茶", "用户表达不确定", null)
            ) : List.of();
            return new ModelResult(ModelMode.OPENAI_GPT55, "REAL_MODEL", "test-model", "测试模型", "测试边界",
                    "继续记录。", ModelMessageType.CONVERSATION, "继续记录。", null,
                    new com.minyuwei.xhs.coffeeagent.agent.application.ConversationModelMessage(
                            List.of("还想补充什么？"), List.of(), List.of(), List.of()),
                    updates, List.of(), List.of(), null, null, null, Instant.now());
        }

        private String latestUserMessageId(ModelContextPackage contextPackage) {
            return contextPackage.currentSession().stream()
                    .filter(entry -> "USER_CONFIRMED".equals(entry.sourceLabel()))
                    .reduce((first, second) -> second)
                    .orElseThrow()
                    .sourceMessageId();
        }
    }
}

