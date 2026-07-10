package com.minyuwei.xhs.coffeeagent.flavor.application;

import com.minyuwei.xhs.coffeeagent.flavor.domain.FlavorSuggestion;
import com.minyuwei.xhs.coffeeagent.support.FakeFlavorSuggestionGenerator;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlavorSuggestionServiceTest {
    @Test
    void mapsModelCandidatesAndPassesSensoryContext() {
        FakeFlavorSuggestionGenerator generator = new FakeFlavorSuggestionGenerator(List.of(
                candidate("佛手柑", "明亮的柑橘皮与花香。", "适合热段香气中的清扬柑橘联想。")
        ));
        FlavorSuggestionService service = new FlavorSuggestionService(generator);

        List<FlavorSuggestion> suggestions = service.suggest(
                "s1",
                "清新的柑橘感",
                TemperatureFlavor.TemperatureStage.HOT,
                TemperatureFlavor.SenseType.AROMA
        );

        assertEquals("清新的柑橘感", generator.lastRequest().inputTerm());
        assertEquals(TemperatureFlavor.TemperatureStage.HOT, generator.lastRequest().temperatureStage());
        assertEquals(TemperatureFlavor.SenseType.AROMA, generator.lastRequest().senseType());
        assertEquals(1, suggestions.size());
        assertEquals("佛手柑", suggestions.getFirst().name());
        assertEquals("明亮的柑橘皮与花香。", suggestions.getFirst().description());
        assertEquals("适合热段香气中的清扬柑橘联想。", suggestions.getFirst().reason());
        assertEquals(FlavorSuggestion.Status.SUGGESTED, suggestions.getFirst().status());
        assertEquals(TemperatureFlavor.Polarity.NEUTRAL, suggestions.getFirst().polarity());
        assertTrue(suggestions.getFirst().sensoryDimensions().isEmpty());
    }

    @Test
    void filtersInvalidCandidatesDeduplicatesNamesAndKeepsAtMostEight() {
        List<FlavorSuggestionGenerator.FlavorCandidate> candidates = new ArrayList<>();
        candidates.add(candidate(" ", "空名称", "应被过滤"));
        candidates.add(candidate("佛手柑", "第一条", "第一条理由"));
        candidates.add(candidate(" 佛手柑 ", "重复条目", "重复理由"));
        candidates.add(candidate("LIME", "青柠感", "英文名称"));
        candidates.add(candidate("lime", "大小写重复", "重复理由"));
        for (int index = 1; index <= 10; index++) {
            candidates.add(candidate("候选" + index, "描述" + index, "理由" + index));
        }
        FlavorSuggestionService service = new FlavorSuggestionService(new FakeFlavorSuggestionGenerator(candidates));

        List<FlavorSuggestion> suggestions = service.suggest(
                "s1",
                "清新",
                TemperatureFlavor.TemperatureStage.COOL,
                TemperatureFlavor.SenseType.TASTE
        );

        assertEquals(8, suggestions.size());
        assertEquals(1, suggestions.stream().filter(item -> item.name().equals("佛手柑")).count());
        assertEquals(1, suggestions.stream().filter(item -> item.name().equalsIgnoreCase("lime")).count());
        assertTrue(suggestions.stream().noneMatch(item -> item.name().isBlank()));
    }

    @Test
    void returnsEmptyInsteadOfInventingCandidatesWhenGeneratorFailsOrReturnsEmpty() {
        FlavorSuggestionService failingService = new FlavorSuggestionService(
                FakeFlavorSuggestionGenerator.failing(new IllegalStateException("模型格式异常"))
        );
        FlavorSuggestionService emptyService = new FlavorSuggestionService(new FakeFlavorSuggestionGenerator(List.of()));

        assertTrue(failingService.suggest(
                "s1",
                "坚果感",
                TemperatureFlavor.TemperatureStage.WARM,
                TemperatureFlavor.SenseType.TASTE
        ).isEmpty());
        assertTrue(emptyService.suggest(
                "s1",
                "坚果感",
                TemperatureFlavor.TemperatureStage.WARM,
                TemperatureFlavor.SenseType.TASTE
        ).isEmpty());
    }

    private FlavorSuggestionGenerator.FlavorCandidate candidate(String name, String description, String reason) {
        return new FlavorSuggestionGenerator.FlavorCandidate(name, description, reason);
    }
}
