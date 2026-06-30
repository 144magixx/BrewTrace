package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.agent.application.AgentOrchestrator;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.infrastructure.TastingSessionRepositoryAdapter;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkbenchFactBoundaryTest {
    @Test
    void insufficientFactsDoNotGenerateDraftsAndCompletedFactsKeepBoundaryNotes() {
        WebWorkbenchService service = new WebWorkbenchService(new TastingSessionApplicationService(
                new TastingSessionRepositoryAdapter(),
                new CurrentUserProvider(),
                new AgentOrchestrator()
        ));
        var created = service.createSession(OrchestrationMode.EXPLICIT_WORKFLOW);

        var insufficient = service.submitMessage(created.sessionId(), "今天喝了一支水洗埃塞，有柑橘和红茶感");
        assertEquals("WAITING_FOR_FACTS", insufficient.status().name());
        assertTrue(insufficient.draftTabs().isEmpty());

        var ready = service.submitMessage(created.sessionId(), "豆子是某烘焙商的埃塞水洗豆，水温 92 度，粉水比 1:15，想看克制、夸张和锐评。");
        assertEquals(3, ready.draftTabs().size());
        assertTrue(ready.recordSummary().suggestedFlavors().contains("甜橙"));
        assertTrue(ready.recordSummary().factBoundaryNotes().stream().anyMatch(note -> note.contains("待确认联想")));
        assertFalse(ready.draftTabs().stream().anyMatch(draft -> draft.body().contains("我喝到甜橙")));
    }
}
