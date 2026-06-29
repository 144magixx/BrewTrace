package com.minyuwei.xhs.coffeeagent.tasting.api;

import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingTemplateApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.BrewRecipe;
import com.minyuwei.xhs.coffeeagent.tasting.domain.CoffeeBean;
import com.minyuwei.xhs.coffeeagent.tasting.domain.SensoryScore;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

import java.util.List;

public class TastingTemplateController {
    private final TastingTemplateApplicationService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public TastingTemplateController(TastingTemplateApplicationService service) {
        this.service = service;
    }

    public ApiResponse<TastingTemplateApplicationService.TemplateSnapshot> save(String requestId, String sessionId, CoffeeBean bean, BrewRecipe recipe, List<SensoryScore> scores, List<TemperatureFlavor> flavors) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.saveTemplate(sessionId, bean, recipe, scores, flavors));
    }
}
