package com.minyuwei.xhs.coffeeagent.flavor.application;

import com.minyuwei.xhs.coffeeagent.flavor.domain.FlavorSuggestion;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

import java.util.List;

public class FlavorSuggestionService {
    public List<FlavorSuggestion> suggest(String sessionId, String inputTerm, TemperatureFlavor.TemperatureStage stage, TemperatureFlavor.SenseType senseType) {
        if ("柑橘".equals(inputTerm)) {
            return List.of(
                    FlavorSuggestion.suggested(sessionId, inputTerm, "柠檬", "明亮、尖锐、清爽，高酸感明显。", stage, senseType),
                    FlavorSuggestion.suggested(sessionId, inputTerm, "青柠", "比柠檬更青绿，带轻微皮感。", stage, senseType),
                    FlavorSuggestion.suggested(sessionId, inputTerm, "甜橙", "圆润、甜感更高，适合中温段描述。", stage, senseType),
                    FlavorSuggestion.suggested(sessionId, inputTerm, "血橙", "带红色水果联想，酸甜更厚。", stage, senseType),
                    FlavorSuggestion.suggested(sessionId, inputTerm, "葡萄柚", "明亮但略带苦韵，适合锐评版参考。", stage, senseType),
                    FlavorSuggestion.suggested(sessionId, inputTerm, "蜜柑", "柔和、甜润、亲和。", stage, senseType),
                    FlavorSuggestion.suggested(sessionId, inputTerm, "柚子", "清透、带皮香和尾段清苦。", stage, senseType)
            );
        }
        return List.of(FlavorSuggestion.suggested(sessionId, inputTerm, inputTerm + "延展", "基于输入词的待确认联想。", stage, senseType));
    }
}
