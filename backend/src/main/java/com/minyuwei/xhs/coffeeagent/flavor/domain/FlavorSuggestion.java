package com.minyuwei.xhs.coffeeagent.flavor.domain;

import com.minyuwei.xhs.coffeeagent.tasting.domain.SensoryScore;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

import java.util.List;
import java.util.UUID;

public record FlavorSuggestion(
        String id,
        String sessionId,
        String inputTerm,
        String name,
        String description,
        TemperatureFlavor.TemperatureStage temperatureStage,
        TemperatureFlavor.SenseType senseType,
        TemperatureFlavor.Polarity polarity,
        List<SensoryScore.Dimension> sensoryDimensions,
        Status status,
        String reason
) {
    public static FlavorSuggestion suggested(String sessionId, String inputTerm, String name, String description, TemperatureFlavor.TemperatureStage stage, TemperatureFlavor.SenseType senseType) {
        return new FlavorSuggestion(UUID.randomUUID().toString(), sessionId, inputTerm, name, description, stage, senseType, TemperatureFlavor.Polarity.POSITIVE, List.of(SensoryScore.Dimension.ACIDITY, SensoryScore.Dimension.AFTERTASTE), Status.SUGGESTED, "由稳定风味词库基于输入词扩展，接受前不是事实。");
    }

    public enum Status {
        SUGGESTED,
        ACCEPTED,
        REJECTED,
        EDITED
    }
}
