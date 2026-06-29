package com.minyuwei.xhs.coffeeagent.trace.infrastructure;

import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTrace;

import java.util.ArrayList;
import java.util.List;

public class AgentTraceRepositoryAdapter {
    private final List<AgentTrace> traces = new ArrayList<>();

    public void save(AgentTrace trace) {
        traces.removeIf(existing -> existing.id().equals(trace.id()));
        traces.add(trace);
    }

    public List<AgentTrace> findBySessionId(String sessionId) {
        return traces.stream().filter(trace -> trace.sessionId().equals(sessionId)).toList();
    }
}
