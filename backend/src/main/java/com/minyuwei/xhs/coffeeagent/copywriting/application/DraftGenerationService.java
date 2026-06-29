package com.minyuwei.xhs.coffeeagent.copywriting.application;

import com.minyuwei.xhs.coffeeagent.agent.application.ContextAssembler;
import com.minyuwei.xhs.coffeeagent.agent.application.DraftAgent;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftCopy;
import com.minyuwei.xhs.coffeeagent.memory.application.MemoryRecallService;
import com.minyuwei.xhs.coffeeagent.memory.domain.MemoryRecall;

import java.util.ArrayList;
import java.util.List;

public class DraftGenerationService {
    private final MemoryRecallService memoryRecallService;
    private final DraftAgent draftAgent = new DraftAgent();

    public DraftGenerationService(MemoryRecallService memoryRecallService) {
        this.memoryRecallService = memoryRecallService;
    }

    public DraftWithMemory generateWithMemory(String sessionId, String query, List<String> confirmedFacts) {
        List<MemoryRecall> recalls = memoryRecallService.recall(sessionId, query, 3);
        List<String> enrichedFacts = new ArrayList<>(confirmedFacts);
        recalls.stream().map(recall -> "历史召回：" + recall.summary()).forEach(enrichedFacts::add);
        List<DraftCopy> drafts = draftAgent.generate(new ContextAssembler.AgentContext(sessionId, enrichedFacts, List.of(), OrchestrationMode.EXPLICIT_WORKFLOW));
        return new DraftWithMemory(drafts, recalls);
    }

    public record DraftWithMemory(List<DraftCopy> drafts, List<MemoryRecall> recalls) {
    }
}
