package com.minyuwei.xhs.coffeeagent.tools.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;

import java.util.Map;

public class XiaohongshuToolAdapterTest {
    public static void run() {
        ToolRegistry registry = new ToolRegistry();
        new XiaohongshuToolRegistrar(new PromptTemplateLoader()).register(registry);
        ApiContractTestSupport.assertTrue(registry.definition("xiaohongshu.clickPublish").orElseThrow().requiresConfirmation(), "公开发布工具必须要求确认");
        ToolAdapter.ToolResult blocked = new XiaohongshuToolAdapter("xiaohongshu.clickPublish").execute(new ToolAdapter.ToolRequest("s1", "公开发布", Map.of(), false));
        ApiContractTestSupport.assertTrue("FAILED".equals(blocked.status()), "缺少确认时小红书公开发布工具不得执行");
    }
}
