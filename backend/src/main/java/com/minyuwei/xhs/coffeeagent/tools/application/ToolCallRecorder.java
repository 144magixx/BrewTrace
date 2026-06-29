package com.minyuwei.xhs.coffeeagent.tools.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ToolCallRecorder {
    private final List<ToolCallRecord> records = new ArrayList<>();
    private final com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder traceRecorder;

    public ToolCallRecorder() {
        this.traceRecorder = null;
    }

    public ToolCallRecorder(com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    public ToolCallRecord record(String sessionId, String toolName, String purpose, Map<String, Object> inputSummary, ToolAdapter.ToolResult result, boolean requiresConfirmation) {
        ToolCallRecord record = new ToolCallRecord(
                UUID.randomUUID().toString(),
                sessionId,
                toolName,
                purpose,
                redact(inputSummary).toString(),
                result.status(),
                redact(result.output()).toString(),
                requiresConfirmation,
                Instant.now()
        );
        records.add(record);
        if (traceRecorder != null) {
            traceRecorder.recordSingleStep(sessionId, com.minyuwei.xhs.coffeeagent.trace.domain.AgentTraceStep.StepType.TOOL_CALL, "工具调用：" + toolName, purpose, Map.of("input", redact(inputSummary), "output", redact(result.output())));
        }
        return record;
    }

    public List<ToolCallRecord> records() {
        return List.copyOf(records);
    }

    private Map<String, Object> redact(Map<String, Object> source) {
        if (source == null) {
            return Map.of();
        }
        return source.entrySet().stream()
                .filter(entry -> !entry.getKey().toLowerCase().contains("key"))
                .filter(entry -> !entry.getKey().equalsIgnoreCase("authorization"))
                .filter(entry -> !entry.getKey().equalsIgnoreCase("cookie"))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public record ToolCallRecord(
            String id,
            String sessionId,
            String toolName,
            String purpose,
            String inputSummary,
            String outputStatus,
            String outputSummary,
            boolean requiresConfirmation,
            Instant createdAt
    ) {
    }
}
