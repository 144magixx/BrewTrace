package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;

public class XiaohongshuToolRegistrar {
    private static final String DESCRIPTIONS_RESOURCE = "prompts/tools/xiaohongshu/descriptions-v1.json";

    private final JsonNode descriptions;

    public XiaohongshuToolRegistrar(PromptTemplateLoader promptTemplateLoader) {
        this.descriptions = promptTemplateLoader.loadJson(DESCRIPTIONS_RESOURCE);
    }

    public void register(ToolRegistry registry) {
        register(registry, "xiaohongshu.checkLogin", false, ToolRegistry.RiskLevel.LOW);
        register(registry, "xiaohongshu.searchFeeds", false, ToolRegistry.RiskLevel.LOW);
        register(registry, "xiaohongshu.getFeedDetail", false, ToolRegistry.RiskLevel.LOW);
        register(registry, "xiaohongshu.fillPublish", true, ToolRegistry.RiskLevel.MEDIUM);
        register(registry, "xiaohongshu.clickPublish", true, ToolRegistry.RiskLevel.HIGH);
        register(registry, "xiaohongshu.saveDraft", true, ToolRegistry.RiskLevel.MEDIUM);
    }

    private void register(ToolRegistry registry, String name, boolean requiresConfirmation, ToolRegistry.RiskLevel riskLevel) {
        registry.register(new ToolRegistry.ToolDefinition(name, descriptions.path(name).asText(), riskLevel, requiresConfirmation), new XiaohongshuToolAdapter(name));
    }
}
