package com.minyuwei.xhs.coffeeagent.tasting.domain;

public record BrewRecipe(
        String dripper,
        String grinder,
        String grindSize,
        double doseGram,
        double waterGram,
        int waterTemperatureCelsius,
        String ratio,
        String pouringPlan,
        int brewTimeSeconds
) {
    public boolean hasCoreParameters() {
        return doseGram > 0 && waterGram > 0 && waterTemperatureCelsius > 0 && ratio != null && !ratio.isBlank();
    }
}
