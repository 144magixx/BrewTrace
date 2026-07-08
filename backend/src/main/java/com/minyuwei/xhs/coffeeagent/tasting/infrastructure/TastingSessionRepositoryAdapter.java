package com.minyuwei.xhs.coffeeagent.tasting.infrastructure;

import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSessionRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class TastingSessionRepositoryAdapter implements TastingSessionRepository {
    private final Map<String, TastingSession> sessions = new ConcurrentHashMap<>();

    @Override
    public TastingSession save(TastingSession session) {
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public Optional<TastingSession> findById(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void deleteById(String sessionId) {
        sessions.remove(sessionId);
    }
}
