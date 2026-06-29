package com.minyuwei.xhs.coffeeagent.tasting.application;

import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.CoffeeAgentException;
import com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory;
import com.minyuwei.xhs.coffeeagent.tasting.domain.BrewRecipe;
import com.minyuwei.xhs.coffeeagent.tasting.domain.CoffeeBean;
import com.minyuwei.xhs.coffeeagent.tasting.domain.SensoryScore;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

import java.util.ArrayList;
import java.util.List;

public class TastingTemplateApplicationService {
    public TemplateSnapshot saveTemplate(String sessionId, CoffeeBean bean, BrewRecipe brewRecipe, List<SensoryScore> scores, List<TemperatureFlavor> flavors) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new CoffeeAgentException(ApiError.of("SESSION_REQUIRED", ErrorCategory.USER_FIXABLE, "保存模板需要会话。", true, "CREATE_SESSION"));
        }
        List<String> validationMessages = new ArrayList<>();
        if (brewRecipe == null || !brewRecipe.hasCoreParameters()) {
            validationMessages.add("冲煮参数仍不完整。");
        }
        List<TemperatureFlavor> acceptedFlavors = flavors == null ? List.of() : flavors.stream()
                .filter(flavor -> flavor.confirmationStatus().name().equals("ACCEPTED") || flavor.confirmationStatus().name().equals("EDITED"))
                .toList();
        return new TemplateSnapshot(sessionId, bean, brewRecipe, List.copyOf(scores == null ? List.of() : scores), acceptedFlavors, validationMessages);
    }

    public record TemplateSnapshot(
            String sessionId,
            CoffeeBean bean,
            BrewRecipe brewRecipe,
            List<SensoryScore> scores,
            List<TemperatureFlavor> acceptedFlavors,
            List<String> validationMessages
    ) {
    }
}
