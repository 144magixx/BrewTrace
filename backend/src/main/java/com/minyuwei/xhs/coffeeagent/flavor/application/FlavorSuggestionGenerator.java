package com.minyuwei.xhs.coffeeagent.flavor.application;

import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

import java.util.List;

public interface FlavorSuggestionGenerator {
    List<FlavorCandidate> generate(GenerationRequest request);

    record GenerationRequest(
            String inputTerm,
            TemperatureFlavor.TemperatureStage temperatureStage,
            TemperatureFlavor.SenseType senseType
    ) {
    }

    record FlavorCandidate(
            String name,
            String description,
            String reason
    ) {
    }
}
