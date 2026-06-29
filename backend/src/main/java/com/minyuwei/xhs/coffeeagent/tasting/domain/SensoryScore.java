package com.minyuwei.xhs.coffeeagent.tasting.domain;

public record SensoryScore(Dimension dimension, int value, String note) {
    public SensoryScore {
        if (value < 0 || value > 10) {
            throw new IllegalArgumentException("感官评分必须在 0-10 之间");
        }
    }

    public enum Dimension {
        ACIDITY,
        SWEETNESS,
        BITTERNESS,
        BODY,
        CLEANNESS,
        AFTERTASTE
    }
}
