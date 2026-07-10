package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.flavor.domain.FlavorSuggestion;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlavorSuggestionToolAdapter implements ToolAdapter {
    public static final String TOOL_NAME = "flavor_suggestion";

    private final FlavorSuggestionService service;

    public FlavorSuggestionToolAdapter(FlavorSuggestionService service) {
        this.service = service;
    }

    @Override
    public String name() {
        return TOOL_NAME;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String inputTerm = value(request.input(), "inputTerm", "");
        if (inputTerm.isBlank()) {
            return ToolResult.failure("FLAVOR_INPUT_TERM_REQUIRED", Map.of("executed", false));
        }
        TemperatureFlavor.TemperatureStage stage = enumValue(
                TemperatureFlavor.TemperatureStage.class,
                value(request.input(), "temperatureStage", TemperatureFlavor.TemperatureStage.HOT.name()),
                TemperatureFlavor.TemperatureStage.HOT
        );
        TemperatureFlavor.SenseType senseType = enumValue(
                TemperatureFlavor.SenseType.class,
                value(request.input(), "senseType", TemperatureFlavor.SenseType.TASTE.name()),
                TemperatureFlavor.SenseType.TASTE
        );
        int limit = limit(request.input().get("limit"));
        List<Map<String, Object>> suggestions = service.suggest(request.sessionId(), inputTerm, stage, senseType).stream()
                .limit(limit)
                .map(this::suggestion)
                .toList();
        return ToolResult.success(Map.of(
                "tool", TOOL_NAME,
                "executed", true,
                "inputTerm", inputTerm,
                "resultBoundary", "PENDING_ASSOCIATION",
                "suggestions", suggestions
        ));
    }

    private Map<String, Object> suggestion(FlavorSuggestion suggestion) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", suggestion.id());
        value.put("name", suggestion.name());
        value.put("description", suggestion.description());
        value.put("temperatureStage", suggestion.temperatureStage().name());
        value.put("senseType", suggestion.senseType().name());
        value.put("polarity", suggestion.polarity().name());
        value.put("sensoryDimensions", suggestion.sensoryDimensions().stream().map(Enum::name).toList());
        value.put("status", suggestion.status().name());
        value.put("basisType", "PENDING_ASSOCIATION");
        value.put("confirmationStatus", "PENDING_CONFIRMATION");
        value.put("sendStatus", "SEND_AFTER_CONFIRMATION");
        value.put("reason", suggestion.reason());
        return value;
    }

    private int limit(Object value) {
        if (value instanceof Number number) {
            return Math.min(Math.max(number.intValue(), 1), 8);
        }
        if (value instanceof String text) {
            try {
                return Math.min(Math.max(Integer.parseInt(text), 1), 8);
            } catch (NumberFormatException ignored) {
                return 6;
            }
        }
        return 6;
    }

    private String value(Map<String, Object> input, String key, String fallback) {
        Object value = input.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
