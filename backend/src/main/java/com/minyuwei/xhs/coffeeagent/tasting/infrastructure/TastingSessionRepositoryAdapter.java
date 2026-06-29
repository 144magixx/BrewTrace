package com.minyuwei.xhs.coffeeagent.tasting.infrastructure;

import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSessionRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class TastingSessionRepositoryAdapter implements TastingSessionRepository {
    private final Map<String, TastingSession> sessions = new LinkedHashMap<>();

    @Override
    public TastingSession save(TastingSession session) {
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public Optional<TastingSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
}
