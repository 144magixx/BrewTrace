package com.minyuwei.xhs.coffeeagent.shared.error;

public class CoffeeAgentException extends RuntimeException {
    private final ApiError apiError;

    public CoffeeAgentException(ApiError apiError) {
        super(apiError.message());
        this.apiError = apiError;
    }

    public ApiError apiError() {
        return apiError;
    }
}
