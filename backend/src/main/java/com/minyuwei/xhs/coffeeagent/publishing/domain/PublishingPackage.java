package com.minyuwei.xhs.coffeeagent.publishing.domain;

import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory;

import java.util.List;
import java.util.UUID;

public class PublishingPackage {
    private final String id;
    private final String sessionId;
    private final String draftId;
    private final String title;
    private final String body;
    private final List<String> tags;
    private final List<String> imageAssetIds;
    private Status status;
    private final List<String> riskChecklist;
    private final String confirmationId;
    private String publishedUrl;

    public PublishingPackage(String sessionId, String draftId, String title, String body, List<String> tags, List<String> imageAssetIds) {
        this.id = UUID.randomUUID().toString();
        this.sessionId = sessionId;
        this.draftId = draftId;
        this.title = title;
        this.body = body;
        this.tags = List.copyOf(tags);
        this.imageAssetIds = List.copyOf(imageAssetIds);
        this.status = Status.DRAFT_PACKAGE;
        this.riskChecklist = List.of("外部参考已标明来源", "未确认风味未写成事实", "公开发布需要二次确认");
        this.confirmationId = UUID.randomUUID().toString();
    }

    public void confirmPackage(boolean confirmed) {
        if (!confirmed) {
            throw safetyBlocked("发布包未确认，不能填写发布页。", "CONFIRM_PACKAGE");
        }
        status = Status.PACKAGE_CONFIRMED;
    }

    public void markXhsFilled() {
        if (status != Status.PACKAGE_CONFIRMED) {
            throw safetyBlocked("需要先确认发布包。", "CONFIRM_PACKAGE");
        }
        status = Status.XHS_FILLED;
    }

    public void publishAfterPreview(boolean confirmedAfterPreview) {
        if (status != Status.XHS_FILLED || !confirmedAfterPreview) {
            throw safetyBlocked("公开发布需要发布页预览后二次确认。", "CONFIRM_AFTER_PREVIEW");
        }
        status = Status.PUBLISHED;
        publishedUrl = "https://www.xiaohongshu.com/explore/published-" + id;
    }

    private CoffeeAgentException safetyBlocked(String message, String nextAction) {
        return new CoffeeAgentException(ApiError.of("PUBLISH_CONFIRMATION_REQUIRED", ErrorCategory.SAFETY_BLOCKED, message, true, nextAction));
    }

    public String id() {
        return id;
    }

    public String sessionId() {
        return sessionId;
    }

    public Status status() {
        return status;
    }

    public String confirmationId() {
        return confirmationId;
    }

    public String publishedUrl() {
        return publishedUrl;
    }

    public List<String> riskChecklist() {
        return riskChecklist;
    }

    public String title() {
        return title;
    }

    public String body() {
        return body;
    }

    public List<String> tags() {
        return tags;
    }

    public List<String> imageAssetIds() {
        return imageAssetIds;
    }

    public enum Status {
        DRAFT_PACKAGE,
        PACKAGE_CONFIRMED,
        XHS_FILLED,
        PREVIEW_CONFIRMED,
        PUBLISHED,
        FAILED,
        CANCELLED
    }
}
