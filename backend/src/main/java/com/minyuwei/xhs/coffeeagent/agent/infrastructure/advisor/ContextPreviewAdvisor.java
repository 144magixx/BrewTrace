package com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelPreview;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.ActualModelRequestCapture;
import com.minyuwei.xhs.coffeeagent.shared.error.SensitiveValueRedactor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ContextPreviewAdvisor implements CallAdvisor {
    private final ActualModelRequestCapture requestCapture;
    private final int order;

    /**
     * 创建读取实际 HTTP 请求体的预览 Advisor。
     *
     * @param requestCapture 发送层共享的实际请求体捕获器
     */
    public ContextPreviewAdvisor(ActualModelRequestCapture requestCapture) {
        this(requestCapture, 300);
    }

    /**
     * 创建指定执行顺序并读取实际 HTTP 请求体的预览 Advisor。
     *
     * @param requestCapture 发送层共享的实际请求体捕获器
     * @param order Advisor 链中的执行顺序
     */
    public ContextPreviewAdvisor(ActualModelRequestCapture requestCapture, int order) {
        this.requestCapture = requestCapture;
        this.order = order;
    }

    /**
     * 在模型调用完成后读取发送层捕获的同一份请求体，仅脱敏后写入响应上下文。
     *
     * @param request 当前 ChatClient 请求
     * @param chain 后续 Advisor 与模型调用链
     * @return 携带请求预览、发送时间和下游上下文的响应
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String modelName = modelName(request.context());
        Instant sentAt = Instant.now();
        Map<String, Object> requestContext = new LinkedHashMap<>(request.context());
        requestContext.put(ModelAdvisorContextKeys.REQUEST_SENT_AT, sentAt);
        ChatClientResponse response = chain.nextCall(request.mutate().context(requestContext).build());
        Map<String, Object> responseContext = new LinkedHashMap<>(requestContext);
        responseContext.putAll(response.context());
        requestCapture.latest().ifPresent(actualRequestBody -> responseContext.put(
                ModelAdvisorContextKeys.REQUEST_PREVIEW,
                new ModelPreview.ModelRequestPreview(
                        "实际发送给大模型的请求",
                        modelName,
                        ModelMode.OPENAI_GPT55.code(),
                        "Spring AI ChatClient -> Responses API",
                        SensitiveValueRedactor.redact(actualRequestBody),
                        "SAFE_TO_DISPLAY",
                        sentAt
                )
        ));
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
