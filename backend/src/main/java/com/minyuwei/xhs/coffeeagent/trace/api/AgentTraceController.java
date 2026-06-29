package com.minyuwei.xhs.coffeeagent.trace.api;

import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceService;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTrace;

import java.util.List;

public class AgentTraceController {
    private final AgentTraceService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public AgentTraceController(AgentTraceService service) {
        this.service = service;
    }

    public ApiResponse<List<AgentTrace>> traces(String requestId, String sessionId) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.traces(sessionId));
    }
}
