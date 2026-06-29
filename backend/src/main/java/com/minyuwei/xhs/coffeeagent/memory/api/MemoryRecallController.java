package com.minyuwei.xhs.coffeeagent.memory.api;

import com.minyuwei.xhs.coffeeagent.memory.application.MemoryRecallService;
import com.minyuwei.xhs.coffeeagent.memory.domain.MemoryRecall;
import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;

import java.util.List;

public class MemoryRecallController {
    private final MemoryRecallService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public MemoryRecallController(MemoryRecallService service) {
        this.service = service;
    }

    public ApiResponse<List<MemoryRecall>> recall(String requestId, String sessionId, String query, int limit) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.recall(sessionId, query, limit));
    }
}
