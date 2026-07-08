package com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelPreview;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiResponsesRequestFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ContextPreviewAdvisor implements CallAdvisor {
    private final OpenAiResponsesRequestFactory requestFactory;
    private final int order;

    public ContextPreviewAdvisor(OpenAiResponsesRequestFactory requestFactory) {
        this(requestFactory, 100);
    }

    public ContextPreviewAdvisor(OpenAiResponsesRequestFactory requestFactory, int order) {
        this.requestFactory = requestFactory;
        this.order = order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String modelName = modelName(request.context());
        Instant sentAt = Instant.now();
        ModelPreview.ModelRequestPreview requestPreview = new ModelPreview.ModelRequestPreview(
                "已通过 Spring AI Advisor 链发送给大模型",
                modelName,
                ModelMode.OPENAI_GPT55.code(),
                "Spring AI ChatClient + Advisor -> Responses API",
                requestFactory.createPreviewBody(modelName, request.prompt()),
                "SAFE_TO_DISPLAY",
                sentAt
        );
        Map<String, Object> requestContext = new LinkedHashMap<>(request.context());
        requestContext.put(ModelAdvisorContextKeys.REQUEST_PREVIEW, requestPreview);
        requestContext.put(ModelAdvisorContextKeys.REQUEST_SENT_AT, sentAt);
        ChatClientResponse response = chain.nextCall(request.mutate().context(requestContext).build());
        Map<String, Object> responseContext = new LinkedHashMap<>(requestContext);
        responseContext.putAll(response.context());
        return response.mutate().context(responseContext).build();
    }

    private String modelName(Map<String, Object> context) {
        Object value = context.get(ModelAdvisorContextKeys.MODEL_NAME);
        return value == null ? "gpt-5.5" : String.valueOf(value);
    }

    @Override
    public String getName() {
        return "coffee-agent-context-preview-advisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
