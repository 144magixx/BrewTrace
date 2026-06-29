package com.minyuwei.xhs.coffeeagent.tasting.domain;

import java.util.Optional;

public interface TastingSessionRepository {
    TastingSession save(TastingSession session);

    Optional<TastingSession> findById(String sessionId);
}
