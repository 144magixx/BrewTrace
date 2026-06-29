package com.minyuwei.xhs.coffeeagent.trace.api;

import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTraceStep;

import java.util.Map;

public class AgentTraceSseTest {
    public static void run() {
        AgentTraceStep step = AgentTraceStep.create("trace-1", 1, AgentTraceStep.StepType.CONTEXT_BUILD, "上下文组装", "已组装用户事实", Map.of());
        AgentTraceSsePublisher.SseEvent event = new AgentTraceSsePublisher().publishStep(step);
        ApiContractTestSupport.assertTrue("trace.step.created".equals(event.event()), "SSE 事件类型必须正确");
        ApiContractTestSupport.assertContains(event.data(), "CONTEXT_BUILD", "SSE data 必须包含步骤类型");
    }
}
