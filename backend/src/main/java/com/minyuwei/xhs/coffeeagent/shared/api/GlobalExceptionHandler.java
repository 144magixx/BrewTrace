package com.minyuwei.xhs.coffeeagent.shared.api;

import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CoffeeAgentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleCoffeeAgentException(CoffeeAgentException exception, HttpServletRequest request) {
        return ApiResponse.failure(requestId(request), exception.apiError());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleRuntimeException(RuntimeException exception, HttpServletRequest request) {
        return ApiResponse.failure(requestId(request), ApiError.of(
                "INTERNAL_ERROR",
                ErrorCategory.RETRYABLE,
                "本地服务暂时不可用，已保留当前内容。",
                true,
                "CHECK_LOCAL_SERVICE",
                "RETRY"
        ));
    }

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

    private String requestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        return requestId == null || requestId.isBlank() ? java.util.UUID.randomUUID().toString() : requestId;
    }
}
