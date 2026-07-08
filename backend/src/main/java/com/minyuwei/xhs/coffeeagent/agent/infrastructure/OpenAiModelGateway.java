package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelAgentMessage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelPreview;
import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;
import com.minyuwei.xhs.coffeeagent.shared.error.SensitiveValueRedactor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAiModelGateway implements ModelGateway {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final OpenAiResponsesLlmClient client;
    private final OpenAiResponsesRequestFactory requestFactory;
    private final OpenAiResponsesParser parser;
    private final String baseUrl;
    private final String modelName;
    private final int timeoutSeconds;
    private final String apiKey;

    public OpenAiModelGateway(OpenAiResponsesLlmClient client, OpenAiResponsesRequestFactory requestFactory, OpenAiResponsesParser parser, String baseUrl, String modelName, int timeoutSeconds, String apiKey) {
        this.client = client;
        this.requestFactory = requestFactory;
        this.parser = parser;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.timeoutSeconds = timeoutSeconds;
        this.apiKey = apiKey;
    }

    @Override
    public ModelResult complete(ModelContextPackage contextPackage) {
        String requestBody = requestFactory.createBody(modelName, contextPackage);
        String requestPreviewBody = requestFactory.createPreviewBody(modelName, contextPackage);
        Instant sentAt = Instant.now();
        ModelPreview.ModelRequestPreview requestPreview = new ModelPreview.ModelRequestPreview(
                "已发送给大模型",
                modelName,
                ModelMode.OPENAI_GPT55.code(),
                "/responses",
                requestPreviewBody,
                "SAFE_TO_DISPLAY",
                sentAt
        );
        try {
            OpenAiResponsesLlmClient.LlmResponse response = client.createResponse(baseUrl, apiKey, requestBody, timeoutSeconds);
            ModelAgentMessage message = parser.parseMessage(response.body());
            Instant receivedAt = Instant.now();
            Map<String, Object> responsePreview = new LinkedHashMap<>();
            responsePreview.put("status", response.status());
            responsePreview.put("messageType", message.messageType());
            responsePreview.put("talk", message.talk());
            responsePreview.put("post", message.post());
            responsePreview.put("conversation", message.conversation());
            responsePreview.put("warnings", message.warnings());
            String responsePreviewBody = SensitiveValueRedactor.redact(previewJson(responsePreview));
            return new ModelResult(
                    ModelMode.OPENAI_GPT55,
                    "REAL_MODEL",
                    modelName,
                    ModelMode.OPENAI_GPT55.displayName(),
                    "由真实模型生成，事实边界仍需检查。",
                    message.talk(),
                    message.messageType(),
                    message.talk(),
                    message.post(),
                    message.conversation(),
                    message.warnings(),
                    message.variants(),
                    requestPreview,
                    new ModelPreview.ModelResponsePreview("大模型返回", modelName, ModelMode.OPENAI_GPT55.code(), responsePreviewBody, "SAFE_TO_DISPLAY", receivedAt),
                    null,
                    receivedAt
            );
        } catch (ModelGatewayException exception) {
            RecoverableModelError error = toRecoverableError(exception, contextPackage.sessionId());
            return errorResult(contextPackage, failureRequestPreview(exception, requestPreview), error);
        }
    }

    private ModelPreview.ModelRequestPreview failureRequestPreview(ModelGatewayException exception, ModelPreview.ModelRequestPreview requestPreview) {
        if (exception.code() == RecoverableModelError.Code.MODEL_AUTH_FAILED && (apiKey == null || apiKey.isBlank())) {
            return new ModelPreview.ModelRequestPreview(
                    "未发送给大模型",
                    requestPreview.modelName(),
                    requestPreview.mode(),
                    requestPreview.endpointPath(),
                    requestPreview.rawJson(),
                    requestPreview.redactionStatus(),
                    null
            );
        }
        return requestPreview;
    }

    private ModelResult errorResult(ModelContextPackage contextPackage, ModelPreview.ModelRequestPreview requestPreview, RecoverableModelError error) {
        return new ModelResult(
                ModelMode.OPENAI_GPT55,
                "ERROR",
                modelName,
                "真实模型不可用",
                "真实模型失败，已保留当前会话和上下文预览。",
                "",
                null,
                "",
                null,
                null,
                java.util.List.of(),
                java.util.List.of(),
                requestPreview,
                new ModelPreview.ModelResponsePreview("大模型返回", modelName, ModelMode.OPENAI_GPT55.code(), previewJson(Map.of("error", error.message(), "code", error.code().name())), "SAFE_TO_DISPLAY", Instant.now()),
                error,
                Instant.now()
        );
    }

    private RecoverableModelError toRecoverableError(ModelGatewayException exception, String sessionId) {
        String message = SensitiveValueRedactor.redact(switch (exception.code()) {
            case MODEL_TIMEOUT -> "真实模型响应超时，请重试。";
            case MODEL_AUTH_FAILED -> "真实模型鉴权失败，请检查本地 OPENAI_API_KEY。";
            case MODEL_RATE_LIMITED -> "真实模型当前限流，请稍后重试。";
            case MODEL_FORMAT_INVALID -> "真实模型返回格式异常，请重试。";
            case MODEL_SERVICE_UNAVAILABLE -> "真实模型服务暂时不可用，请稍后重试。";
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
