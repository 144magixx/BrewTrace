package com.minyuwei.xhs.coffeeagent.tasting.api;

import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;
import com.minyuwei.xhs.coffeeagent.tasting.application.ArchiveCoffeeRecordService;

import java.util.List;

public class ArchiveController {
    private final ArchiveCoffeeRecordService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public ArchiveController(ArchiveCoffeeRecordService service) {
        this.service = service;
    }

    public ApiResponse<ArchiveCoffeeRecordService.ArchiveResult> archive(String requestId, String sessionId, String finalDraftId, List<String> flavorKeywords) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.archive(sessionId, finalDraftId, flavorKeywords, true));
    }
}
