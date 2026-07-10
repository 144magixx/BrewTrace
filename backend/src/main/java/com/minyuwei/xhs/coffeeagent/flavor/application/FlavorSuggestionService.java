package com.minyuwei.xhs.coffeeagent.flavor.application;

import com.minyuwei.xhs.coffeeagent.flavor.domain.FlavorSuggestion;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FlavorSuggestionService {
    private static final int MAX_CANDIDATES = 8;

    private final FlavorSuggestionGenerator generator;

    public FlavorSuggestionService(FlavorSuggestionGenerator generator) {
        this.generator = generator;
    }

    public List<FlavorSuggestion> suggest(String sessionId, String inputTerm, TemperatureFlavor.TemperatureStage stage, TemperatureFlavor.SenseType senseType) {
        if (inputTerm == null || inputTerm.isBlank()) {
            return List.of();
        }

        List<FlavorSuggestionGenerator.FlavorCandidate> generated;
        try {
            generated = generator.generate(new FlavorSuggestionGenerator.GenerationRequest(inputTerm, stage, senseType));
        } catch (RuntimeException exception) {
            return List.of();
        }
        if (generated == null || generated.isEmpty()) {
            return List.of();
        }

        Map<String, FlavorSuggestionGenerator.FlavorCandidate> uniqueCandidates = new LinkedHashMap<>();
        for (FlavorSuggestionGenerator.FlavorCandidate candidate : generated) {
            FlavorSuggestionGenerator.FlavorCandidate normalized = normalize(candidate);
            if (normalized == null) {
                continue;
            }
            uniqueCandidates.putIfAbsent(normalized.name().toLowerCase(Locale.ROOT), normalized);
            if (uniqueCandidates.size() == MAX_CANDIDATES) {
                break;
            }
        }

        return uniqueCandidates.values().stream()
                .map(candidate -> FlavorSuggestion.pendingAssociation(
                        sessionId,
                        inputTerm,
                        candidate.name(),
                        candidate.description(),
                        stage,
                        senseType,
                        candidate.reason()
                ))
                .toList();
    }

    private FlavorSuggestionGenerator.FlavorCandidate normalize(FlavorSuggestionGenerator.FlavorCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        String name = trim(candidate.name());
        String description = trim(candidate.description());
        String reason = trim(candidate.reason());
        if (name.isBlank() || description.isBlank() || reason.isBlank()) {
            return null;
        }
        return new FlavorSuggestionGenerator.FlavorCandidate(name, description, reason);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
