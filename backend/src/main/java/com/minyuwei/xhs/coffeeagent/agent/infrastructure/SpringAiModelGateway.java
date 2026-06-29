package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTraceStep;

import java.util.List;
import java.util.Map;

public class SpringAiModelGateway implements ModelGateway {
    private final AgentTraceRecorder traceRecorder;

    public SpringAiModelGateway() {
        this.traceRecorder = null;
    }

    public SpringAiModelGateway(AgentTraceRecorder traceRecorder) {
        this.traceRecorder = traceRecorder;
    }

    @Override
    public ModelResult complete(ModelRequest request) {
        String content = "模型适配器占位响应：" + request.purpose() + "；事实=" + String.join("、", request.facts());
        if (traceRecorder != null) {
            traceRecorder.recordSingleStep("model-gateway", AgentTraceStep.StepType.MODEL_CALL, "模型调用", request.purpose(), Map.of("prompt", request.prompt(), "facts", request.facts()));
        }
        return new ModelResult(content, List.of("当前环境未接入真实 Spring AI，使用离线适配器。"));
    }
}
