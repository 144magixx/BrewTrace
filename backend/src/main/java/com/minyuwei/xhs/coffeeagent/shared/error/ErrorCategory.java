package com.minyuwei.xhs.coffeeagent.shared.error;

public enum ErrorCategory {
    USER_FIXABLE,
    RETRYABLE,
    DEGRADED,
    FATAL,
    SAFETY_BLOCKED
}
