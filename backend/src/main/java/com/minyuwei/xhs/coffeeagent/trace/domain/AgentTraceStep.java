package com.minyuwei.xhs.coffeeagent.trace.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record AgentTraceStep(
        String id,
        String traceId,
        int sequence,
        StepType stepType,
        String title,
        String summary,
        Map<String, Object> promptSnapshot,
        Map<String, Object> modelOutputSnapshot,
        Map<String, Object> toolInputSnapshot,
        Map<String, Object> toolOutputSnapshot,
        Map<String, Object> memorySnapshot,
        String decision,
        String toolSelectionReason,
        Instant createdAt
) {
    public static AgentTraceStep create(String traceId, int sequence, StepType stepType, String title, String summary, Map<String, Object> snapshot) {
        Map<String, Object> safe = redact(snapshot);
        return new AgentTraceStep(UUID.randomUUID().toString(), traceId, sequence, stepType, title, summary,
                stepType == StepType.MODEL_CALL ? safe : Map.of(),
                stepType == StepType.MODEL_CALL ? safe : Map.of(),
                stepType == StepType.TOOL_CALL ? safe : Map.of(),
                stepType == StepType.TOOL_CALL ? safe : Map.of(),
                stepType == StepType.MEMORY_RECALL ? safe : Map.of(),
                "", "", Instant.now());
    }

    public static Map<String, Object> redact(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> safe = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalized = key.toLowerCase();
            if (normalized.contains("apikey") || normalized.contains("api_key") || normalized.contains("authorization") || normalized.contains("cookie") || normalized.contains("token")) {
                safe.put(key, "[REDACTED]");
            } else {
                safe.put(key, value);
            }
        });
        return safe;
    }

    public enum StepType {
        USER_INPUT,
        CONTEXT_BUILD,
        MODEL_CALL,
        TOOL_CALL,
        MEMORY_RECALL,
        REVIEW,
        PUBLISH_CONFIRMATION,
        SYSTEM_STATUS
    }
}
