package com.minyuwei.xhs.coffeeagent.trace.domain;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AgentTrace {
    private final String id;
    private final String sessionId;
    private final String traceType;
    private final OrchestrationMode orchestrationMode;
    private final Instant startedAt;
    private Instant endedAt;
    private String finalDecision;
    private Status status;
    private final List<AgentTraceStep> steps = new ArrayList<>();

    public AgentTrace(String sessionId, String traceType, OrchestrationMode orchestrationMode) {
        this.id = UUID.randomUUID().toString();
        this.sessionId = sessionId;
        this.traceType = traceType;
        this.orchestrationMode = orchestrationMode;
        this.startedAt = Instant.now();
        this.status = Status.RUNNING;
    }

    public AgentTraceStep addStep(AgentTraceStep.StepType type, String title, String summary, java.util.Map<String, Object> snapshot) {
        AgentTraceStep step = AgentTraceStep.create(id, steps.size() + 1, type, title, summary, snapshot);
        steps.add(step);
        return step;
    }

    public void complete(String decision) {
        this.finalDecision = decision;
        this.endedAt = Instant.now();
        this.status = Status.COMPLETED;
    }

    public String id() {
        return id;
    }

    public String sessionId() {
        return sessionId;
    }

    public String traceType() {
        return traceType;
    }

    public OrchestrationMode orchestrationMode() {
        return orchestrationMode;
    }

    public List<AgentTraceStep> steps() {
        return List.copyOf(steps);
    }

    public Status status() {
        return status;
    }

    public String finalDecision() {
        return finalDecision;
    }

    public enum Status {
        RUNNING,
        COMPLETED,
        FAILED,
        DEGRADED
    }
}
