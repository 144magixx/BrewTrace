package com.minyuwei.xhs.coffeeagent.tools.application;

import java.util.Map;

public interface ToolAdapter {
    String name();

    ToolResult execute(ToolRequest request);

    record ToolRequest(String sessionId, String purpose, Map<String, Object> input, boolean confirmed) {
    }

    record ToolResult(String status, Map<String, Object> output, String errorCode) {
        public static ToolResult success(Map<String, Object> output) {
            return new ToolResult("SUCCESS", output, null);
        }

        public static ToolResult failure(String errorCode, Map<String, Object> output) {
            return new ToolResult("FAILED", output, errorCode);
        }
    }
}
