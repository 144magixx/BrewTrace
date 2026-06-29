package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;

import java.util.Map;

public class XiaohongshuToolAdapter implements ToolAdapter {
    private final String name;

    public XiaohongshuToolAdapter(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        if (name.equals("xiaohongshu.clickPublish") && !request.confirmed()) {
            return ToolResult.failure("PUBLISH_CONFIRMATION_REQUIRED", Map.of("executed", false));
        }
        return ToolResult.success(Map.of("tool", name, "executed", true, "sourcePlatform", "xiaohongshu"));
    }
}
