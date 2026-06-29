package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.publishing.domain.GeneratedImageAsset;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;

import java.util.Map;

public class ImageGenerationToolAdapter implements ToolAdapter {
    @Override
    public String name() {
        return "image.generate";
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        if (!Boolean.TRUE.equals(request.input().get("userInitiated"))) {
            return ToolResult.failure("IMAGE_GENERATION_REQUIRES_USER_REQUEST", Map.of("executed", false));
        }
        GeneratedImageAsset asset = GeneratedImageAsset.create(request.sessionId(), String.valueOf(request.input().get("draftId")), String.valueOf(request.input().get("userPrompt")));
        return ToolResult.success(Map.of("assetId", asset.id(), "filePath", asset.filePath(), "executed", true));
    }
}
