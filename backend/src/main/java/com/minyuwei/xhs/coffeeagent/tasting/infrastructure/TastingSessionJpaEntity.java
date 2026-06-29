package com.minyuwei.xhs.coffeeagent.tasting.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;

public record TastingSessionJpaEntity(
        String id,
        String userId,
        String status,
        String orchestrationMode,
        String activeDraftId
) {
    public static TastingSessionJpaEntity fromDomain(TastingSession session) {
        return new TastingSessionJpaEntity(
                session.id(),
                session.userId(),
                session.status().name(),
                session.orchestrationMode().name(),
                session.activeDraftId()
        );
    }

    public OrchestrationMode parsedMode() {
        return OrchestrationMode.valueOf(orchestrationMode);
    }
}
