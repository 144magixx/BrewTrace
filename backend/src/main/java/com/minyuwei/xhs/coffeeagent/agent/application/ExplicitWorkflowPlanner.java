package com.minyuwei.xhs.coffeeagent.agent.application;

import java.util.List;

public class ExplicitWorkflowPlanner {
    public List<String> plan(ContextAssembler.AgentContext context) {
        return List.of("INTERVIEW_MISSING_FACTS", "GENERATE_DRAFTS", "REVIEW_FACT_BOUNDARY");
    }
}
