package com.minyuwei.xhs.coffeeagent.tasting.domain;

import com.minyuwei.xhs.coffeeagent.shared.domain.ConfirmationStatus;
import com.minyuwei.xhs.coffeeagent.shared.domain.SourceType;

public record TemperatureFlavor(
        TemperatureStage temperatureStage,
        SenseType senseType,
        String flavorName,
        String description,
        Polarity polarity,
        SourceType sourceType,
        ConfirmationStatus confirmationStatus
) {
    public enum TemperatureStage {
        HOT,
        WARM,
        COOL
    }

    public enum SenseType {
        AROMA,
        TASTE
    }

    public enum Polarity {
        POSITIVE,
        NEUTRAL,
        NEGATIVE
    }
}
