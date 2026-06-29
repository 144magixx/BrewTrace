package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;

import java.util.Map;

public class ImageGenerationToolAdapterTest {
    public static void run() {
        ImageGenerationToolAdapter adapter = new ImageGenerationToolAdapter();
        ToolAdapter.ToolResult blocked = adapter.execute(new ToolAdapter.ToolRequest("s1", "自动生图", Map.of("draftId", "d1", "userPrompt", "甜橙"), false));
        ApiContractTestSupport.assertTrue("FAILED".equals(blocked.status()), "用户未主动请求时不得生图");
        ToolAdapter.ToolResult generated = adapter.execute(new ToolAdapter.ToolRequest("s1", "用户主动生图", Map.of("draftId", "d1", "userPrompt", "甜橙和红茶", "userInitiated", true), true));
        ApiContractTestSupport.assertTrue(Boolean.TRUE.equals(generated.output().get("executed")), "用户主动请求时可生成图片候选");
    }
}
