package com.minyuwei.xhs.coffeeagent.shared.api;

import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;

public record ApiResponse<T>(String requestId, T data, ApiError error) {
    public static <T> ApiResponse<T> success(String requestId, T data) {
        return new ApiResponse<>(requestId, data, null);
    }

    public static <T> ApiResponse<T> failure(String requestId, ApiError error) {
        return new ApiResponse<>(requestId, null, error);
    }
}
