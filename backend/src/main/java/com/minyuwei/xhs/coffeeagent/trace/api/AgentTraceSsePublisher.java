package com.minyuwei.xhs.coffeeagent.trace.api;

import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTraceStep;

import java.util.ArrayList;
import java.util.List;

public class AgentTraceSsePublisher {
    private final List<SseEvent> events = new ArrayList<>();

    public SseEvent publishStep(AgentTraceStep step) {
        SseEvent event = new SseEvent("trace.step.created", step.id(), "{\"traceId\":\"" + step.traceId() + "\",\"stepType\":\"" + step.stepType() + "\",\"summary\":\"" + step.summary() + "\"}");
        events.add(event);
        return event;
    }

    public List<SseEvent> events() {
        return List.copyOf(events);
    }

    public record SseEvent(String event, String id, String data) {
    }
}
