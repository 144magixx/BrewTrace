package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallPolicy;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallRecorder;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.LinkedHashMap;
import java.util.Map;

public class SpringAiToolCallbackAdapter implements ToolCallback {
    public static final String CONTEXT_SESSION_ID = "sessionId";
    public static final String CONTEXT_PURPOSE = "purpose";
    public static final String CONTEXT_CONFIRMED = "confirmed";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ToolRegistry registry;
    private final ToolCallPolicy policy;
    private final ToolCallRecorder recorder;
    private final String toolName;

    public SpringAiToolCallbackAdapter(ToolRegistry registry, ToolCallPolicy policy, ToolCallRecorder recorder, String toolName) {
        this.registry = registry;
        this.policy = policy;
        this.recorder = recorder;
        this.toolName = toolName;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        ToolRegistry.ToolDefinition definition = definition();
        return DefaultToolDefinition.builder()
                .name(definition.name())
                .description(definition.description())
                .inputSchema(definition.inputSchema())
                .build();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().returnDirect(false).build();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, new ToolContext(Map.of()));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        ToolRegistry.ToolDefinition definition = definition();
        Map<String, Object> input = parseInput(toolInput);
        Map<String, Object> context = toolContext == null ? Map.of() : toolContext.getContext();
        String sessionId = stringValue(input.getOrDefault(CONTEXT_SESSION_ID, context.getOrDefault(CONTEXT_SESSION_ID, "")));
        String purpose = stringValue(input.getOrDefault(CONTEXT_PURPOSE, context.getOrDefault(CONTEXT_PURPOSE, "模型自主调用工具")));
        boolean confirmed = booleanValue(input.getOrDefault(CONTEXT_CONFIRMED, context.getOrDefault(CONTEXT_CONFIRMED, false)));

        policy.verify(definition, confirmed);
        ToolAdapter adapter = registry.adapter(toolName).orElseThrow(() -> new IllegalArgumentException("工具未注册：" + toolName));
        ToolAdapter.ToolResult result = adapter.execute(new ToolAdapter.ToolRequest(sessionId, purpose, input, confirmed));
        recorder.record(sessionId, toolName, purpose, input, result, definition.requiresConfirmation());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tool", toolName);
        response.put("status", result.status());
        response.put("resultBoundary", definition.resultBoundary().name());
        response.put("sideEffectType", definition.sideEffectType().name());
        response.put("requiresConfirmation", definition.requiresConfirmation());
        response.put("output", result.output());
        response.put("errorCode", result.errorCode());
        return toJson(response);
    }

    private ToolRegistry.ToolDefinition definition() {
        return registry.definition(toolName).orElseThrow(() -> new IllegalArgumentException("工具未注册：" + toolName));
    }

    private Map<String, Object> parseInput(String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(OBJECT_MAPPER.readValue(toolInput, MAP_TYPE));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("工具入参必须是 JSON object。", exception);
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(String.valueOf(value));
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("工具结果 JSON 序列化失败", exception);
        }
    }
}
