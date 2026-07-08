package com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.shared.error.SensitiveValueRedactor;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder;
import com.minyuwei.xhs.coffeeagent.trace.domain.AgentTraceStep;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class AgentTraceAdvisor implements CallAdvisor {
    private final AgentTraceRecorder traceRecorder;
    private final int order;

    public AgentTraceAdvisor(AgentTraceRecorder traceRecorder) {
        this(traceRecorder, 200);
    }

    public AgentTraceAdvisor(AgentTraceRecorder traceRecorder, int order) {
        this.traceRecorder = traceRecorder;
        this.order = order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Instant startedAt = Instant.now();
        try {
            ChatClientResponse response = chain.nextCall(request);
            Instant receivedAt = Instant.now();
            String responseContent = content(response.chatResponse());
            Map<String, Object> responseContext = new LinkedHashMap<>(request.context());
            responseContext.putAll(response.context());
            responseContext.put(ModelAdvisorContextKeys.RESPONSE_RECEIVED_AT, receivedAt);
            responseContext.put(ModelAdvisorContextKeys.RESPONSE_CONTENT, responseContent);
            responseContext.put(ModelAdvisorContextKeys.CALL_DURATION_MS, Duration.between(startedAt, receivedAt).toMillis());
            record(request.context(), responseContext, "SUCCESS", null);
            responseContext.put(ModelAdvisorContextKeys.TRACE_RECORDED, true);
            return response.mutate().context(responseContext).build();
        } catch (RuntimeException exception) {
            Map<String, Object> failureContext = new LinkedHashMap<>(request.context());
            failureContext.put(ModelAdvisorContextKeys.CALL_DURATION_MS, Duration.between(startedAt, Instant.now()).toMillis());
            failureContext.put("error", SensitiveValueRedactor.redact(exception.getMessage()));
            record(request.context(), failureContext, "ERROR", exception);
            throw exception;
        }
    }

    private void record(Map<String, Object> requestContext, Map<String, Object> responseContext, String outcome, RuntimeException exception) {
        String sessionId = sessionId(requestContext);
        String modelName = String.valueOf(requestContext.getOrDefault(ModelAdvisorContextKeys.MODEL_NAME, "gpt-5.5"));
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("outcome", outcome);
        snapshot.put("modelName", modelName);
        snapshot.put("transport", "Spring AI ChatClient + Advisor");
        snapshot.put("durationMs", responseContext.getOrDefault(ModelAdvisorContextKeys.CALL_DURATION_MS, 0L));
        snapshot.put("factBoundary", responseContext.getOrDefault(ModelAdvisorContextKeys.FACT_BOUNDARY_SUMMARY, Map.of()));
        snapshot.put("requestPreview", responseContext.getOrDefault(ModelAdvisorContextKeys.REQUEST_PREVIEW, Map.of()));
        if (responseContext.containsKey(ModelAdvisorContextKeys.RESPONSE_CONTENT)) {
            snapshot.put("responseSummary", responseSummary(String.valueOf(responseContext.get(ModelAdvisorContextKeys.RESPONSE_CONTENT))));
        }
        if (exception != null) {
            snapshot.put("error", SensitiveValueRedactor.redact(exception.getMessage()));
        }
        traceRecorder.recordSingleStep(
                sessionId,
                AgentTraceStep.StepType.MODEL_CALL,
                "Spring AI Advisor 模型调用",
                "模型调用结果：" + outcome,
                snapshot
        );
    }

    private String responseSummary(String content) {
        String safe = SensitiveValueRedactor.redact(content == null ? "" : content.trim());
        if (safe.length() <= 240) {
            return safe;
        }
        return safe.substring(0, 240) + "...";
    }

    private String sessionId(Map<String, Object> context) {
        Object value = context.get(ModelAdvisorContextKeys.MODEL_CONTEXT_PACKAGE);
        if (value instanceof ModelContextPackage contextPackage) {
            return contextPackage.sessionId();
        }
        return "unknown-session";
    }

    private String content(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        return chatResponse.getResult().getOutput().getText();
    }

    @Override
    public String getName() {
        return "coffee-agent-trace-advisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
