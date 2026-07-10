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
        return pendingAssociation(sessionId, inputTerm, name, description, stage, senseType, "由风味联想生成器提供，接受前不是事实。");
    }

    public static FlavorSuggestion pendingAssociation(
            String sessionId,
            String inputTerm,
            String name,
            String description,
            TemperatureFlavor.TemperatureStage stage,
            TemperatureFlavor.SenseType senseType,
            String reason
    ) {
        return new FlavorSuggestion(
                UUID.randomUUID().toString(),
                sessionId,
                inputTerm,
                name,
                description,
                stage,
                senseType,
                TemperatureFlavor.Polarity.NEUTRAL,
                List.of(),
                Status.SUGGESTED,
                reason
        );
    }

    public enum Status {
        SUGGESTED,
        ACCEPTED,
        REJECTED,
        EDITED
    }
}
