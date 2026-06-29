package com.minyuwei.xhs.coffeeagent.agent.application;

import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;

import java.util.List;

public class ContextAssembler {
    public AgentContext assemble(TastingSession session) {
        return new AgentContext(session.id(), session.confirmedFacts(), List.of("甜橙", "青柠", "葡萄柚"), session.orchestrationMode());
    }

    public record AgentContext(String sessionId, List<String> confirmedFacts, List<String> unconfirmedFlavorCandidates, OrchestrationMode mode) {
    }
}
