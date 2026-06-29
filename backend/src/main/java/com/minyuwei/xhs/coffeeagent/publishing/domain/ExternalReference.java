package com.minyuwei.xhs.coffeeagent.publishing.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExternalReference(
        String id,
        String sessionId,
        String sourcePlatform,
        String sourceTitle,
        String sourceUrl,
        String query,
        String summary,
        List<String> matchedKeywords,
        Instant createdAt
) {
    public static ExternalReference xhs(String sessionId, String query, int index) {
        return new ExternalReference(UUID.randomUUID().toString(), sessionId, "xiaohongshu", "小红书参考 " + index, "https://www.xiaohongshu.com/explore/ref-" + index, query, "外部参考摘要，必须标明不是用户事实。", List.of("咖啡", "风味"), Instant.now());
    }
}
