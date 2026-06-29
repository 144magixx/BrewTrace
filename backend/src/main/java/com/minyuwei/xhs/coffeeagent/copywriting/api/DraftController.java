package com.minyuwei.xhs.coffeeagent.copywriting.api;

import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftCopy;
import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;

import java.util.List;

public class DraftController {
    private final TastingSessionApplicationService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public DraftController(TastingSessionApplicationService service) {
        this.service = service;
    }

    public ApiResponse<List<DraftCopy>> generateDrafts(String requestId, String sessionId) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.generateDrafts(sessionId));
    }
}
