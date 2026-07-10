package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallPolicy;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallRecorder;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

public class SpringAiToolCallbackAdapterTest {
    public static void run() {
        ToolRegistry registry = new ToolRegistry();
        new FlavorSuggestionToolRegistrar().register(registry, new FlavorSuggestionService());
        ToolCallRecorder recorder = new ToolCallRecorder();
        SpringAiToolCallbackAdapter callback = new SpringAiToolCallbackAdapter(
                registry,
                new ToolCallPolicy(),
                recorder,
                FlavorSuggestionToolAdapter.TOOL_NAME
        );

        String result = callback.call(
                "{\"inputTerm\":\"柑橘\",\"temperatureStage\":\"HOT\",\"senseType\":\"TASTE\",\"limit\":3}",
                new ToolContext(Map.of("sessionId", "s1", "purpose", "模型自主补充候选风味"))
        );

        ApiContractTestSupport.assertContains(callback.getToolDefinition().inputSchema(), "inputTerm", "Spring AI 工具 schema 必须暴露 inputTerm");
        ApiContractTestSupport.assertContains(result, "PENDING_ASSOCIATION", "Spring AI 工具返回必须声明待确认联想边界");
        ApiContractTestSupport.assertContains(result, "甜橙", "Spring AI 工具调用应返回风味候选");
        ApiContractTestSupport.assertTrue(recorder.records().size() == 1, "Spring AI 工具调用必须进入 ToolCallRecorder");
        ApiContractTestSupport.assertTrue("flavor_suggestion".equals(recorder.records().getFirst().toolName()), "工具调用记录必须保留工具名");
    }
}
