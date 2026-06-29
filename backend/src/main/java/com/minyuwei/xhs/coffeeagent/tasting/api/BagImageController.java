package com.minyuwei.xhs.coffeeagent.tasting.api;

import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;
import com.minyuwei.xhs.coffeeagent.tasting.application.BagImageExtractionService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.BagImageAsset;

public class BagImageController {
    private final BagImageExtractionService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public BagImageController(BagImageExtractionService service) {
        this.service = service;
    }

    public ApiResponse<BagImageAsset> upload(String requestId, String sessionId, String filePath, String mimeType) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.extract(sessionId, filePath, mimeType));
    }
}
