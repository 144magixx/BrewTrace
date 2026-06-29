package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;

import java.util.Map;

public class FakeToolAdapter implements ToolAdapter {
    private final String name;

    public FakeToolAdapter(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        return ToolResult.success(Map.of("echo", request.input(), "confirmed", request.confirmed()));
    }
}
