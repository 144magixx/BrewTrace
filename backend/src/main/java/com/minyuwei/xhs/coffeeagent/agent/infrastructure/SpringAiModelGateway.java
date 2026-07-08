package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelAgentMessage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelPreview;
import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor.ModelAdvisorContextKeys;
import com.minyuwei.xhs.coffeeagent.shared.error.SensitiveValueRedactor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class SpringAiModelGateway implements ModelGateway {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChatClient chatClient;
    private final OpenAiResponsesRequestFactory requestFactory;
    private final OpenAiResponsesParser parser;
    private final String modelName;

    public SpringAiModelGateway(ChatClient chatClient, OpenAiResponsesRequestFactory requestFactory, OpenAiResponsesParser parser, String modelName) {
        this.chatClient = chatClient;
        this.requestFactory = requestFactory;
        this.parser = parser;
        this.modelName = modelName;
    }

    @Override
    public ModelResult complete(ModelContextPackage contextPackage) {
        Prompt prompt = requestFactory.createPrompt(contextPackage);
        ModelPreview.ModelRequestPreview fallbackRequestPreview = fallbackRequestPreview(prompt);
        try {
            ChatClientResponse chatClientResponse = chatClient.prompt(prompt)
                    .advisors(advisor -> advisor.params(Map.of(
                            ModelAdvisorContextKeys.MODEL_CONTEXT_PACKAGE, contextPackage,
                            ModelAdvisorContextKeys.MODEL_NAME, modelName
                    )))
                    .call()
                    .chatClientResponse();
            String responseContent = responseContent(chatClientResponse.chatResponse());
            ModelAgentMessage message = parser.parseMessage(responseContent);
            Instant receivedAt = Instant.now();
            Map<String, Object> responsePreview = new LinkedHashMap<>();
            responsePreview.put("transport", "Spring AI ChatClient");
            responsePreview.put("messageType", message.messageType());
            responsePreview.put("talk", message.talk());
            responsePreview.put("post", message.post());
            responsePreview.put("conversation", message.conversation());
            responsePreview.put("warnings", message.warnings());
            responsePreview.put("advisorTraceRecorded", chatClientResponse.context().getOrDefault(ModelAdvisorContextKeys.TRACE_RECORDED, false));
            responsePreview.put("durationMs", chatClientResponse.context().getOrDefault(ModelAdvisorContextKeys.CALL_DURATION_MS, 0L));
            String responsePreviewBody = SensitiveValueRedactor.redact(previewJson(responsePreview));
            ModelPreview.ModelRequestPreview requestPreview = requestPreview(chatClientResponse.context(), fallbackRequestPreview);
            return new ModelResult(
                    ModelMode.OPENAI_GPT55,
                    "REAL_MODEL",
                    modelName,
                    "Spring AI 模型调用",
                    "由 Spring AI ChatClient 调用真实模型生成，事实边界仍需检查。",
                    message.talk(),
                    message.messageType(),
                    message.talk(),
                    message.post(),
                    message.conversation(),
                    message.warnings(),
                    message.variants(),
                    requestPreview,
                    new ModelPreview.ModelResponsePreview("Spring AI 模型返回", modelName, ModelMode.OPENAI_GPT55.code(), responsePreviewBody, "SAFE_TO_DISPLAY", receivedAt),
                    null,
                    receivedAt
            );
        } catch (ModelGatewayException exception) {
            RecoverableModelError error = toRecoverableError(exception, contextPackage.sessionId());
            return errorResult(fallbackRequestPreview, error);
        } catch (Exception exception) {
            RecoverableModelError error = RecoverableModelError.of(
                    RecoverableModelError.Code.MODEL_SERVICE_UNAVAILABLE,
                    SensitiveValueRedactor.redact("Spring AI 模型调用失败，请稍后重试。"),
                    contextPackage.sessionId(),
                    "RETRY"
            );
            return errorResult(fallbackRequestPreview, error);
        }
    }

    private ModelPreview.ModelRequestPreview fallbackRequestPreview(Prompt prompt) {
        return new ModelPreview.ModelRequestPreview(
                "已通过 Spring AI 发送给大模型",
                modelName,
                ModelMode.OPENAI_GPT55.code(),
                "Spring AI ChatClient -> Responses API",
                requestFactory.createPreviewBody(modelName, prompt),
                "SAFE_TO_DISPLAY",
                Instant.now()
        );
    }

    private ModelPreview.ModelRequestPreview requestPreview(Map<String, Object> context, ModelPreview.ModelRequestPreview fallback) {
        Object value = context.get(ModelAdvisorContextKeys.REQUEST_PREVIEW);
        return value instanceof ModelPreview.ModelRequestPreview preview ? preview : fallback;
    }

    private String responseContent(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return "";
        }
        return chatResponse.getResult().getOutput().getText();
    }

    private ModelResult errorResult(ModelPreview.ModelRequestPreview requestPreview, RecoverableModelError error) {
        return new ModelResult(
                ModelMode.OPENAI_GPT55,
                "ERROR",
                modelName,
                "Spring AI 模型不可用",
                "Spring AI 模型调用失败，已保留当前会话和上下文预览。",
                "",
                null,
                "",
                null,
                null,
                java.util.List.of(),
                java.util.List.of(),
                requestPreview,
                new ModelPreview.ModelResponsePreview("Spring AI 模型返回", modelName, ModelMode.OPENAI_GPT55.code(), previewJson(Map.of("error", error.message(), "code", error.code().name())), "SAFE_TO_DISPLAY", Instant.now()),
                error,
                Instant.now()
        );
    }

    private RecoverableModelError toRecoverableError(ModelGatewayException exception, String sessionId) {
        String message = SensitiveValueRedactor.redact(switch (exception.code()) {
            case MODEL_TIMEOUT -> "Spring AI 模型响应超时，请重试。";
            case MODEL_AUTH_FAILED -> "Spring AI 模型鉴权失败，请检查本地 OPENAI_API_KEY。";
            case MODEL_RATE_LIMITED -> "Spring AI 模型当前限流，请稍后重试。";
            case MODEL_FORMAT_INVALID -> "Spring AI 模型返回格式异常，请重试。";
            case MODEL_SERVICE_UNAVAILABLE -> "Spring AI 模型服务暂时不可用，请稍后重试。";
        });
        String[] actions = switch (exception.code()) {
            case MODEL_AUTH_FAILED -> new String[]{"CHECK_LOCAL_ENV", "RETRY"};
            case MODEL_RATE_LIMITED -> new String[]{"TRY_LATER", "RETRY"};
            default -> new String[]{"RETRY"};
        };
        return RecoverableModelError.of(exception.code(), message, sessionId, actions);
    }

    private String previewJson(Object value) {
        try {
            return SensitiveValueRedactor.redact(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value));
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
