package com.minyuwei.xhs.coffeeagent.flavor.application;

import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

public class FlavorSuggestionAgentTest {
    public static void run() {
        var suggestions = new FlavorSuggestionService().suggest("s1", "柑橘", TemperatureFlavor.TemperatureStage.HOT, TemperatureFlavor.SenseType.TASTE);
        ApiContractTestSupport.assertTrue(suggestions.size() >= 5, "柑橘必须返回至少 5 个具体候选");
        String names = suggestions.stream().map(suggestion -> suggestion.name()).toList().toString();
        ApiContractTestSupport.assertContains(names, "甜橙", "候选应包含甜橙");
        ApiContractTestSupport.assertContains(suggestions.getFirst().reason(), "接受前不是事实", "风味候选必须声明事实边界");
    }
}
