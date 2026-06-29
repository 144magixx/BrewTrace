package com.minyuwei.xhs.coffeeagent.tasting.api;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;

import java.util.Map;

public class TastingSessionController {
    private final TastingSessionApplicationService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public TastingSessionController(TastingSessionApplicationService service) {
        this.service = service;
    }

    public ApiResponse<Map<String, String>> createSession(String requestId, OrchestrationMode mode) {
        String currentRequestId = requestIdFilter.begin(requestId);
        var session = service.createSession(mode == null ? OrchestrationMode.EXPLICIT_WORKFLOW : mode);
        return ApiResponse.success(currentRequestId, Map.of("sessionId", session.id(), "status", session.status().name()));
    }

    public ApiResponse<TastingSessionApplicationService.MessageResult> submitMessage(String requestId, String sessionId, String content) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.submitMessage(sessionId, content));
    }

    public ApiResponse<TastingSessionApplicationService.WorkspaceSnapshot> workspace(String requestId, String sessionId) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.workspace(sessionId));
    }
}
