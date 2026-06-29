package com.minyuwei.xhs.coffeeagent.tools.application;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ToolRegistry {
    private final Map<String, ToolDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, ToolAdapter> adapters = new LinkedHashMap<>();

    public void register(ToolDefinition definition, ToolAdapter adapter) {
        definitions.put(definition.name(), definition);
        adapters.put(definition.name(), adapter);
    }

    public Optional<ToolDefinition> definition(String name) {
        return Optional.ofNullable(definitions.get(name));
    }

    public Optional<ToolAdapter> adapter(String name) {
        return Optional.ofNullable(adapters.get(name));
    }

    public Collection<ToolDefinition> allDefinitions() {
        return definitions.values();
    }

    public record ToolDefinition(String name, String description, RiskLevel riskLevel, boolean requiresConfirmation) {
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
