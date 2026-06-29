package com.minyuwei.xhs.coffeeagent.publishing.application;

import com.minyuwei.xhs.coffeeagent.publishing.domain.PublishingPackage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PublishingPackageService {
    private final Map<String, PublishingPackage> packages = new LinkedHashMap<>();

    public PublishingPackage create(String sessionId, String draftId, List<String> imageAssetIds) {
        PublishingPackage publishingPackage = new PublishingPackage(sessionId, draftId, "咖啡品鉴记录", "正文来自已确认事实和明确标记的联想。", List.of("咖啡", "手冲"), imageAssetIds);
        packages.put(publishingPackage.id(), publishingPackage);
        return publishingPackage;
    }

    public PublishingPackage fillXhs(String packageId, boolean confirmed) {
        PublishingPackage publishingPackage = packages.get(packageId);
        publishingPackage.confirmPackage(confirmed);
        publishingPackage.markXhsFilled();
        return publishingPackage;
    }

    public PublishingPackage publishXhs(String packageId, boolean confirmedAfterPreview) {
        PublishingPackage publishingPackage = packages.get(packageId);
        publishingPackage.publishAfterPreview(confirmedAfterPreview);
        return publishingPackage;
    }
}
