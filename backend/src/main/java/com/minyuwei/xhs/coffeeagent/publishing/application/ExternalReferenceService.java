package com.minyuwei.xhs.coffeeagent.publishing.application;

import com.minyuwei.xhs.coffeeagent.publishing.domain.ExternalReference;

import java.util.List;
import java.util.stream.IntStream;

public class ExternalReferenceService {
    public List<ExternalReference> search(String sessionId, String query, int limit) {
        int capped = Math.min(Math.max(limit, 1), 5);
        return IntStream.rangeClosed(1, capped).mapToObj(index -> ExternalReference.xhs(sessionId, query, index)).toList();
    }
}
