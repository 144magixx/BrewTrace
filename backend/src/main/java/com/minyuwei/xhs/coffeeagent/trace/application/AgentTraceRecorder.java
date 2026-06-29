package com.minyuwei.xhs.coffeeagent.trace.application;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTrace;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTraceStep;

import java.util.Map;

public class AgentTraceRecorder {
    private final AgentTraceService traceService;

    public AgentTraceRecorder(AgentTraceService traceService) {
        this.traceService = traceService;
    }

    public AgentTrace recordSingleStep(String sessionId, AgentTraceStep.StepType type, String title, String summary, Map<String, Object> snapshot) {
        AgentTrace trace = traceService.start(sessionId, "WORKFLOW", OrchestrationMode.EXPLICIT_WORKFLOW);
        trace.addStep(type, title, summary, snapshot);
        trace.complete(summary);
        traceService.save(trace);
        return trace;
    }
}
