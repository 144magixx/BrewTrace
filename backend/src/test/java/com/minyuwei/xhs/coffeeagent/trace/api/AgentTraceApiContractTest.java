package com.minyuwei.xhs.coffeeagent.trace.api;

import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceService;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTraceStep;
import com.minyuwei.xhs.coffeeagent.trace.infrastructure.AgentTraceRepositoryAdapter;

import java.util.Map;

public class AgentTraceApiContractTest {
    public static void run() {
        AgentTraceRepositoryAdapter repository = new AgentTraceRepositoryAdapter();
        AgentTraceService service = new AgentTraceService(repository);
        new AgentTraceRecorder(service).recordSingleStep("s1", AgentTraceStep.StepType.MODEL_CALL, "模型调用", "生成风味候选", Map.of("prompt", "hello"));
        var response = new AgentTraceController(service).traces("req-trace", "s1");
        ApiContractTestSupport.assertTrue(response.error() == null, "轨迹 API 必须返回成功 envelope");
        ApiContractTestSupport.assertTrue(response.data().getFirst().steps().getFirst().summary().contains("生成风味候选"), "轨迹卡片必须包含摘要");
    }
}
