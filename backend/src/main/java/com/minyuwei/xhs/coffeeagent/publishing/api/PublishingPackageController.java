package com.minyuwei.xhs.coffeeagent.publishing.api;

import com.minyuwei.xhs.coffeeagent.publishing.application.PublishingPackageService;
import com.minyuwei.xhs.coffeeagent.publishing.domain.PublishingPackage;
import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;

import java.util.List;

public class PublishingPackageController {
    private final PublishingPackageService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public PublishingPackageController(PublishingPackageService service) {
        this.service = service;
    }

    public ApiResponse<PublishingPackage> create(String requestId, String sessionId, String draftId, List<String> imageAssetIds) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.create(sessionId, draftId, imageAssetIds));
    }

    public ApiResponse<PublishingPackage> fillXhs(String requestId, String packageId, boolean confirmed) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.fillXhs(packageId, confirmed));
    }

    public ApiResponse<PublishingPackage> publishXhs(String requestId, String packageId, boolean confirmedAfterPreview) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.publishXhs(packageId, confirmedAfterPreview));
    }
}
