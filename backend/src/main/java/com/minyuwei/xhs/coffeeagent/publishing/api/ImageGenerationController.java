package com.minyuwei.xhs.coffeeagent.publishing.api;

import com.minyuwei.xhs.coffeeagent.publishing.domain.GeneratedImageAsset;
import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;

public class ImageGenerationController {
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public ApiResponse<GeneratedImageAsset> generate(String requestId, String sessionId, String draftId, String prompt) {
        return ApiResponse.success(requestIdFilter.begin(requestId), GeneratedImageAsset.create(sessionId, draftId, prompt));
    }
}
