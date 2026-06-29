package com.minyuwei.xhs.coffeeagent.flavor.api;

import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.flavor.domain.FlavorSuggestion;
import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

import java.util.List;

public class FlavorSuggestionController {
    private final FlavorSuggestionService service;
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public FlavorSuggestionController(FlavorSuggestionService service) {
        this.service = service;
    }

    public ApiResponse<List<FlavorSuggestion>> suggest(String requestId, String sessionId, String inputTerm, TemperatureFlavor.TemperatureStage stage, TemperatureFlavor.SenseType senseType) {
        return ApiResponse.success(requestIdFilter.begin(requestId), service.suggest(sessionId, inputTerm, stage, senseType));
    }
}
