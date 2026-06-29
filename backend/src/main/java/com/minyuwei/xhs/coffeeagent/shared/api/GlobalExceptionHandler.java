package com.minyuwei.xhs.coffeeagent.shared.api;

import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory;

public class GlobalExceptionHandler {
    public ApiResponse<Void> handle(String requestId, RuntimeException exception) {
        if (exception instanceof CoffeeAgentException coffeeAgentException) {
            return ApiResponse.failure(requestId, coffeeAgentException.apiError());
        }
        return ApiResponse.failure(requestId, ApiError.of(
                "INTERNAL_ERROR",
                ErrorCategory.FATAL,
                "系统暂时不可用，已保留当前内容。",
                false,
                "RETRY_LATER"
        ));
    }
}
