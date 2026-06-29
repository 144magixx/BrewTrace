package com.minyuwei.xhs.coffeeagent.tasting.domain;

import com.minyuwei.xhs.coffeeagent.shared.domain.ConfirmationStatus;
import com.minyuwei.xhs.coffeeagent.shared.domain.SourceType;

public record CoffeeBean(
        String name,
        String roaster,
        String origin,
        String variety,
        String process,
        String roastLevel,
        String batch,
        String roastDate,
        SourceType sourceType,
        ConfirmationStatus confirmationStatus
) {
    public static CoffeeBean confirmed(String name, String roaster, String origin, String process) {
        return new CoffeeBean(name, roaster, origin, "", process, "", "", "", SourceType.USER_CONFIRMED, ConfirmationStatus.CONFIRMED);
    }
}
