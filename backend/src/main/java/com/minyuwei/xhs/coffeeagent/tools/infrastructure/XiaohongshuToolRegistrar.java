package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;

public class XiaohongshuToolRegistrar {
    public void register(ToolRegistry registry) {
        register(registry, "xiaohongshu.checkLogin", false, ToolRegistry.RiskLevel.LOW);
        register(registry, "xiaohongshu.searchFeeds", false, ToolRegistry.RiskLevel.LOW);
        register(registry, "xiaohongshu.getFeedDetail", false, ToolRegistry.RiskLevel.LOW);
        register(registry, "xiaohongshu.fillPublish", true, ToolRegistry.RiskLevel.MEDIUM);
        register(registry, "xiaohongshu.clickPublish", true, ToolRegistry.RiskLevel.HIGH);
        register(registry, "xiaohongshu.saveDraft", true, ToolRegistry.RiskLevel.MEDIUM);
    }

    private void register(ToolRegistry registry, String name, boolean requiresConfirmation, ToolRegistry.RiskLevel riskLevel) {
        registry.register(new ToolRegistry.ToolDefinition(name, name + " 工具", riskLevel, requiresConfirmation), new XiaohongshuToolAdapter(name));
    }
}
