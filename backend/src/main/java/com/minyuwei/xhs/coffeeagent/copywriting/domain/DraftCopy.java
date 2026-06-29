package com.minyuwei.xhs.coffeeagent.copywriting.domain;

import java.util.List;
import java.util.UUID;

public record DraftCopy(
        String id,
        String sessionId,
        Style style,
        String title,
        String body,
        List<String> tags,
        List<String> factBoundaryNotes,
        List<String> reviewWarnings,
        int satisfactionScore,
        Status status
) {
    public static DraftCopy create(String sessionId, Style style, String title, String body, List<String> factBoundaryNotes, List<String> warnings) {
        return new DraftCopy(UUID.randomUUID().toString(), sessionId, style, title, body, List.of("咖啡", "手冲", "品鉴"), factBoundaryNotes, warnings, 0, Status.GENERATED);
    }

    public boolean writesUnconfirmedFlavorAsFact(List<String> unconfirmedFlavors) {
        return unconfirmedFlavors.stream().anyMatch(flavor -> body.contains("我喝到" + flavor) || body.contains("明确是" + flavor));
    }

    public enum Style {
        RESTRAINED,
        EXAGGERATED,
        SHARP_REVIEW
    }

    public enum Status {
        GENERATED,
        ACCEPTED,
        ARCHIVED
    }
}
