package com.minyuwei.xhs.coffeeagent.tools.application;

import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory;

public class ToolCallPolicy {
    public void verify(ToolRegistry.ToolDefinition definition, boolean confirmed) {
        if (definition.requiresConfirmation() && !confirmed) {
            throw new CoffeeAgentException(ApiError.of(
                    "TOOL_CONFIRMATION_REQUIRED",
                    ErrorCategory.SAFETY_BLOCKED,
                    "该工具需要用户确认后才能执行。",
                    true,
                    "CONFIRM_ACTION"
            ));
        }
    }
}
