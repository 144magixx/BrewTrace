package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.ConversationMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentContextPreviewTest {
    @Test
    void previewGroupsCurrentSessionAndConfirmedFactsForGpt55Request() {
        AgentStateAssembler assembler = new AgentStateAssembler(new FactBoundaryChecker());
        var state = assembler.assemble(new TastingSessionApplicationService.WorkspaceSnapshot(
                "s1",
                "今天喝了什么咖啡？",
                OrchestrationMode.EXPLICIT_WORKFLOW,
                List.of(ConversationMessage.user("s1", "今天喝了一支水洗埃塞，有柑橘和红茶感")),
                List.of(),
                List.of("处理法：水洗", "产地：埃塞", "用户确认风味：柑橘", "用户确认风味：红茶")
        ));

        assertTrue(state.contextPreview().sections().stream().anyMatch(section -> section.sectionType().equals("CURRENT_SESSION")));
        assertTrue(state.contextPreview().sections().stream().anyMatch(section -> section.sectionType().equals("CONFIRMED_FACTS")));
        assertEquals(5, state.contextPreview().willSendCount());
        assertTrue(state.pendingAssociations().isEmpty());
        assertTrue(state.candidateMemories().isEmpty());
    }
}
