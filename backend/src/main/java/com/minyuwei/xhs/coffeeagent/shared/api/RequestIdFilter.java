package com.minyuwei.xhs.coffeeagent.shared.api;

import java.util.UUID;

public class RequestIdFilter {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public String begin(String providedRequestId) {
        String requestId = providedRequestId == null || providedRequestId.isBlank()
                ? UUID.randomUUID().toString()
                : providedRequestId;
        CURRENT.set(requestId);
        return requestId;
    }

    public String current() {
        String requestId = CURRENT.get();
        return requestId == null ? begin(null) : requestId;
    }

    public void clear() {
        CURRENT.remove();
    }
}
