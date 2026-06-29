package com.minyuwei.xhs.coffeeagent.trace.application;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTrace;
import com.minyuwei.xhs.coffeeagent.trace.infrastructure.AgentTraceRepositoryAdapter;

import java.util.List;

public class AgentTraceService {
    private final AgentTraceRepositoryAdapter repository;

    public AgentTraceService(AgentTraceRepositoryAdapter repository) {
        this.repository = repository;
    }

    public AgentTrace start(String sessionId, String traceType, OrchestrationMode mode) {
        AgentTrace trace = new AgentTrace(sessionId, traceType, mode);
        repository.save(trace);
        return trace;
    }

    public void save(AgentTrace trace) {
        repository.save(trace);
    }

    public List<AgentTrace> traces(String sessionId) {
        return repository.findBySessionId(sessionId);
    }
}
