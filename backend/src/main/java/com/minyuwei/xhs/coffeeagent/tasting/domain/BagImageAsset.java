package com.minyuwei.xhs.coffeeagent.tasting.domain;

import com.minyuwei.xhs.coffeeagent.shared.domain.ConfirmationStatus;

import java.util.Map;
import java.util.UUID;

public record BagImageAsset(
        String id,
        String sessionId,
        String filePath,
        String mimeType,
        String ocrText,
        Map<String, String> extractedBeanFields,
        ConfirmationStatus confirmationStatus
) {
    public static BagImageAsset pending(String sessionId, String filePath, String mimeType, Map<String, String> fields) {
        return new BagImageAsset(UUID.randomUUID().toString(), sessionId, filePath, mimeType, "", fields, ConfirmationStatus.PENDING_CONFIRMATION);
    }
}
