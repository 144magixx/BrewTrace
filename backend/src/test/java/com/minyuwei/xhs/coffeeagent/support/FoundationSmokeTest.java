package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;
import com.minyuwei.xhs.coffeeagent.shared.config.CoffeeAgentProperties;
import com.minyuwei.xhs.coffeeagent.shared.error.ApiError;
import com.minyuwei.xhs.coffeeagent.shared.error.ErrorCategory;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolAdapter;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallPolicy;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallRecorder;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;

import java.util.Map;

public class FoundationSmokeTest {
    public static void run() {
        RequestIdFilter requestIdFilter = new RequestIdFilter();
        String requestId = requestIdFilter.begin(null);
        ApiResponse<String> success = ApiResponse.success(requestId, "ok");
        ApiContractTestSupport.assertTrue(success.error() == null, "成功响应 error 必须为空");

        ApiResponse<Void> failure = ApiResponse.failure(requestId, ApiError.of("X", ErrorCategory.USER_FIXABLE, "需要用户处理", true));
        ApiContractTestSupport.assertTrue(failure.data() == null, "失败响应 data 必须为空");

        CoffeeAgentProperties properties = CoffeeAgentProperties.defaults();
        ApiContractTestSupport.assertTrue(properties.embedding().dimensions() == 1024, "Embedding 默认维度必须为 1024");

        ToolRegistry.ToolDefinition definition = new ToolRegistry.ToolDefinition("xhs.publish", "公开发布", ToolRegistry.RiskLevel.HIGH, true);
        boolean blocked = false;
        try {
            new ToolCallPolicy().verify(definition, false);
        } catch (RuntimeException expected) {
            blocked = true;
        }
        ApiContractTestSupport.assertTrue(blocked, "高影响工具缺少确认时必须阻断");

        ToolCallRecorder recorder = new ToolCallRecorder();
        recorder.record("s1", "fake", "脱敏检查", Map.of("apiKey", "secret", "keyword", "咖啡"), ToolAdapter.ToolResult.success(Map.of("Authorization", "token", "status", "ok")), false);
        String summary = recorder.records().getFirst().inputSummary() + recorder.records().getFirst().outputSummary();
        ApiContractTestSupport.assertTrue(!summary.contains("secret") && !summary.contains("token"), "工具记录不得包含密钥或授权信息");
    }
}
