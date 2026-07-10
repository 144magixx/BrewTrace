package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;

public class FlavorSuggestionToolRegistrar {
    private static final String DEFINITION_RESOURCE = "prompts/tools/flavor-suggestion/definition-v1.json";

    private final PromptTemplateLoader promptTemplateLoader;

    public FlavorSuggestionToolRegistrar(PromptTemplateLoader promptTemplateLoader) {
        this.promptTemplateLoader = promptTemplateLoader;
    }

    public void register(ToolRegistry registry, FlavorSuggestionService service) {
        JsonNode definition = promptTemplateLoader.loadJson(DEFINITION_RESOURCE);
        registry.register(
                new ToolRegistry.ToolDefinition(
                        FlavorSuggestionToolAdapter.TOOL_NAME,
                        definition.path("description").asText(),
                        ToolRegistry.RiskLevel.LOW,
                        false,
                        definition.path("inputSchema").toPrettyString(),
                        definition.path("outputSchema").toPrettyString(),
                        ToolRegistry.ResultBoundary.PENDING_ASSOCIATION,
                        ToolRegistry.SideEffectType.NONE,
                        true
                ),
                new FlavorSuggestionToolAdapter(service)
        );
    }
}
