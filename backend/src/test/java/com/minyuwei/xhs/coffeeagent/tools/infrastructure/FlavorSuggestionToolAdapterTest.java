package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;

import java.util.Map;

public class FlavorSuggestionToolAdapterTest {
    public static void run() {
        FlavorSuggestionToolAdapter adapter = new FlavorSuggestionToolAdapter(new FlavorSuggestionService());

        ToolAdapter.ToolResult result = adapter.execute(new ToolAdapter.ToolRequest(
                "s1",
                "补充待确认风味联想",
                Map.of("inputTerm", "柑橘", "temperatureStage", "WARM", "senseType", "TASTE", "limit", 4),
                false
        ));

        ApiContractTestSupport.assertTrue("SUCCESS".equals(result.status()), "风味联想工具应执行成功");
        ApiContractTestSupport.assertTrue("PENDING_ASSOCIATION".equals(result.output().get("resultBoundary")), "风味联想结果只能作为待确认联想");
        String output = result.output().toString();
        ApiContractTestSupport.assertContains(output, "甜橙", "柑橘联想应包含甜橙候选");
        ApiContractTestSupport.assertContains(output, "SEND_AFTER_CONFIRMATION", "风味联想发送状态必须等待用户确认");
    }
}
