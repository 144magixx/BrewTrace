package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionGenerator;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.support.FakeFlavorSuggestionGenerator;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class FlavorSuggestionToolAdapterTest {
    @Test
    void adapterKeepsFinalLimitAndPendingAssociationBoundary() {
        verifyAdapterContract();
    }

    public static void run() {
        verifyAdapterContract();
    }

    private static void verifyAdapterContract() {
        FlavorSuggestionToolAdapter adapter = new FlavorSuggestionToolAdapter(new FlavorSuggestionService(
                new FakeFlavorSuggestionGenerator(List.of(
                        candidate("佛手柑"),
                        candidate("甜橙"),
                        candidate("葡萄柚"),
                        candidate("柚子"),
                        candidate("蜜柑")
                ))
        ));

        ToolAdapter.ToolResult result = adapter.execute(new ToolAdapter.ToolRequest(
                "s1",
                "补充待确认风味联想",
                Map.of("inputTerm", "柑橘", "temperatureStage", "WARM", "senseType", "TASTE", "limit", 2),
                false
        ));

        ApiContractTestSupport.assertTrue("SUCCESS".equals(result.status()), "风味联想工具应执行成功");
        ApiContractTestSupport.assertTrue("PENDING_ASSOCIATION".equals(result.output().get("resultBoundary")), "风味联想结果只能作为待确认联想");
        ApiContractTestSupport.assertTrue(((List<?>) result.output().get("suggestions")).size() == 2, "Adapter 必须继续执行最终 limit");
        String output = result.output().toString();
        ApiContractTestSupport.assertContains(output, "甜橙", "柑橘联想应包含甜橙候选");
        ApiContractTestSupport.assertContains(output, "SEND_AFTER_CONFIRMATION", "风味联想发送状态必须等待用户确认");
    }

    private static FlavorSuggestionGenerator.FlavorCandidate candidate(String name) {
        return new FlavorSuggestionGenerator.FlavorCandidate(name, name + "描述", name + "联想理由");
    }
}
