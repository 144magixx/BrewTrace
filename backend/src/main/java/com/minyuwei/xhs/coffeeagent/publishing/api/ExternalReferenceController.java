package com.minyuwei.xhs.coffeeagent.publishing.api;

import com.minyuwei.xhs.coffeeagent.publishing.application.ExternalReferenceService;
import com.minyuwei.xhs.coffeeagent.publishing.domain.ExternalReference;
import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;

import java.util.List;

public class ExternalReferenceController {
    private final ExternalReferenceService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public ExternalReferenceController(ExternalReferenceService service) {
        this.service = service;
    }

    public ApiResponse<List<ExternalReference>> search(String requestId, String sessionId, String query, int limit) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.search(sessionId, query, limit));
    }
}
