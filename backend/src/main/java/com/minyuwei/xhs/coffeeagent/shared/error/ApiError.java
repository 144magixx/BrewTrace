package com.minyuwei.xhs.coffeeagent.shared.error;

import java.util.List;
import java.util.Map;

public record ApiError(
        String code,
        ErrorCategory category,
        String message,
        boolean recoverable,
        List<String> nextActions,
        Map<String, Object> details
) {
    public static ApiError of(String code, ErrorCategory category, String message, boolean recoverable, String... nextActions) {
        return new ApiError(code, category, message, recoverable, List.of(nextActions), Map.of());
    }

    public ApiError withDetails(Map<String, Object> details) {
        return new ApiError(code, category, message, recoverable, nextActions, details);
    }
}
