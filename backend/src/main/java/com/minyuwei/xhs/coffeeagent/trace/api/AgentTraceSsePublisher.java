package com.minyuwei.xhs.coffeeagent.trace.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTraceStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AgentTraceSsePublisher {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final List<SseEvent> events = new ArrayList<>();

    public SseEvent publishStep(AgentTraceStep step) {
        SseEvent event = new SseEvent("trace.step.created", step.id(), toJson(Map.of(
                "traceId", step.traceId(),
                "stepType", step.stepType(),
                "summary", step.summary()
        )));
        events.add(event);
        return event;
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("SSE 事件 JSON 序列化失败", exception);
        }
    }

    public List<SseEvent> events() {
        return List.copyOf(events);
    }

    public record SseEvent(String event, String id, String data) {
    }
}
