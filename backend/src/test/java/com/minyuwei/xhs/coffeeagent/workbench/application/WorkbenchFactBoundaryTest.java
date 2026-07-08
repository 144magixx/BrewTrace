package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelMessageType;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.support.FakeModelGateway;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.infrastructure.TastingSessionRepositoryAdapter;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkbenchFactBoundaryTest {
    @Test
    void workbenchUsesGpt55AndDoesNotGenerateOfflineMockDraftsWithoutKey() {
        WebWorkbenchService service = new WebWorkbenchService(new TastingSessionApplicationService(
                new TastingSessionRepositoryAdapter(),
                new CurrentUserProvider()
        ), new AgentStateAssembler(new FactBoundaryChecker()));
        var created = service.createSession(OrchestrationMode.EXPLICIT_WORKFLOW);

        var result = service.submitMessage(created.sessionId(), "今天喝了一支水洗埃塞，有柑橘和红茶感");
        assertTrue(result.draftTabs().isEmpty());
        assertTrue(result.recordSummary().suggestedFlavors().isEmpty());
        assertTrue(result.agentState().pendingAssociations().isEmpty());
        assertTrue(result.agentState().candidateMemories().isEmpty());
        assertTrue(result.agentState().modelOutput().recoverableError().code().name().equals("MODEL_AUTH_FAILED"));
    }

    @Test
    void conversationMessageKeepsWaitingAndDoesNotCreateDraftTabs() {
        WebWorkbenchService service = new WebWorkbenchService(newSessionService(), new AgentStateAssembler(new FactBoundaryChecker(), new FakeModelGateway(ModelMessageType.CONVERSATION)));
        var created = service.createSession(OrchestrationMode.EXPLICIT_WORKFLOW);

        var result = service.submitMessage(created.sessionId(), "今天喝了一杯咖啡");

        assertEquals(com.minyuwei.xhs.coffeeagent.workbench.domain.WebWorkbenchSession.Status.WAITING_FOR_FACTS, result.status());
        assertTrue(result.draftTabs().isEmpty());
        assertEquals(ModelMessageType.CONVERSATION, result.agentState().modelOutput().messageType());
        assertTrue(result.agentState().modelOutput().talk().contains("最明显的风味"));
        assertEquals(1, result.recordSummary().pendingQuestions().size());
        assertFalse(result.recordSummary().pendingQuestions().isEmpty());
    }

    @Test
    void postMessageCreatesDraftTabsFromPostOnly() {
        WebWorkbenchService service = new WebWorkbenchService(newSessionService(), new AgentStateAssembler(new FactBoundaryChecker(), new FakeModelGateway(ModelMessageType.POST)));
        var created = service.createSession(OrchestrationMode.EXPLICIT_WORKFLOW);

        var result = service.submitMessage(created.sessionId(), "今天喝了一支水洗埃塞，橙柑和红茶感明显，没有更多补充了，给我生成文案吧。");

        assertEquals(com.minyuwei.xhs.coffeeagent.workbench.domain.WebWorkbenchSession.Status.DRAFTS_READY, result.status());
        assertEquals(3, result.draftTabs().size());
        assertEquals(ModelMessageType.POST, result.agentState().modelOutput().messageType());
        assertTrue(result.agentState().modelOutput().talk().contains("三版文案"));
    }

    private TastingSessionApplicationService newSessionService() {
        return new TastingSessionApplicationService(
                new TastingSessionRepositoryAdapter(),
                new CurrentUserProvider()
        );
    }
}
