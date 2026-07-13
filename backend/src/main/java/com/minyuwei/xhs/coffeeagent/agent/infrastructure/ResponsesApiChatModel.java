package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

public class ResponsesApiChatModel implements ChatModel {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OpenAiResponsesLlmClient client;
    private final OpenAiResponsesRequestFactory requestFactory;
    private final String baseUrl;
    private final String modelName;
    private final int timeoutSeconds;
    private final String apiKey;
    private final List<ToolCallback> toolCallbacks;
    private final ActualModelRequestCapture requestCapture;

    /**
     * 创建不暴露工具的 Responses API 模型适配器。
     *
     * @param client 实际执行 HTTP 请求的客户端
     * @param requestFactory 将 Prompt 序列化为 Responses API 请求体的工厂
     * @param baseUrl 模型服务基础地址
     * @param modelName 实际模型名称
     * @param timeoutSeconds 单次 HTTP 请求超时秒数
     * @param apiKey 本地运行环境提供的模型鉴权值
     */
    public ResponsesApiChatModel(OpenAiResponsesLlmClient client, OpenAiResponsesRequestFactory requestFactory, String baseUrl, String modelName, int timeoutSeconds, String apiKey) {
        this(client, requestFactory, baseUrl, modelName, timeoutSeconds, apiKey, List.of(), new ActualModelRequestCapture());
    }

    /**
     * 创建带受控工具集合的 Responses API 模型适配器，并使用独立请求捕获器。
     *
     * @param client 实际执行 HTTP 请求的客户端
     * @param requestFactory 将 Prompt 序列化为 Responses API 请求体的工厂
     * @param baseUrl 模型服务基础地址
     * @param modelName 实际模型名称
     * @param timeoutSeconds 单次 HTTP 请求超时秒数
     * @param apiKey 本地运行环境提供的模型鉴权值
     * @param toolCallbacks 允许发送给模型的工具回调
     */
    public ResponsesApiChatModel(OpenAiResponsesLlmClient client, OpenAiResponsesRequestFactory requestFactory, String baseUrl, String modelName, int timeoutSeconds, String apiKey, List<ToolCallback> toolCallbacks) {
        this(client, requestFactory, baseUrl, modelName, timeoutSeconds, apiKey, toolCallbacks, new ActualModelRequestCapture());
    }

    /**
     * 创建与请求预览链路共享实际发送体捕获器的 Responses API 模型适配器。
     *
     * @param client 实际执行 HTTP 请求的客户端
     * @param requestFactory 将 Prompt 序列化为 Responses API 请求体的工厂
     * @param baseUrl 模型服务基础地址
     * @param modelName 实际模型名称
     * @param timeoutSeconds 单次 HTTP 请求超时秒数
     * @param apiKey 本地运行环境提供的模型鉴权值
     * @param toolCallbacks 允许发送给模型的工具回调
     * @param requestCapture 保存实际交给 HTTP Client 的请求体
     */
    public ResponsesApiChatModel(
            OpenAiResponsesLlmClient client,
            OpenAiResponsesRequestFactory requestFactory,
            String baseUrl,
            String modelName,
            int timeoutSeconds,
            String apiKey,
            List<ToolCallback> toolCallbacks,
            ActualModelRequestCapture requestCapture
    ) {
        this.client = client;
        this.requestFactory = requestFactory;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.timeoutSeconds = timeoutSeconds;
        this.apiKey = apiKey;
        this.toolCallbacks = toolCallbacks == null ? List.of() : List.copyOf(toolCallbacks);
        this.requestCapture = requestCapture;
    }

    /**
     * 序列化并记录实际请求体，然后调用 Responses API 并转换模型响应。
     *
     * @param prompt Spring AI 传入的本轮 Prompt
     * @return 标准 Spring AI ChatResponse
     */
    @Override
    public ChatResponse call(Prompt prompt) {
        String requestBody = requestFactory.createBody(modelName, prompt, toolCallbacks);
        requestCapture.record(requestBody);
        OpenAiResponsesLlmClient.LlmResponse response = client.createResponse(baseUrl, apiKey, requestBody, timeoutSeconds);
        return toChatResponse(response.body());
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.error(new ModelGatewayException(RecoverableModelError.Code.MODEL_SERVICE_UNAVAILABLE, "当前 Responses 兼容模型暂不支持流式返回。"));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptions.builder().model(modelName).build();
    }

    @Override
    public ChatOptions getOptions() {
        return getDefaultOptions();
    }

    private ChatResponse toChatResponse(String responseBody) {
        List<AssistantMessage.ToolCall> toolCalls = toolCalls(responseBody);
        if (!toolCalls.isEmpty()) {
            AssistantMessage message = AssistantMessage.builder()
                    .content("")
                    .toolCalls(toolCalls)
                    .build();
            return new ChatResponse(List.of(new Generation(message, ChatGenerationMetadata.builder().finishReason("tool_calls").build())));
        }
        return new ChatResponse(List.of(new Generation(new AssistantMessage(responseBody))));
    }

    private List<AssistantMessage.ToolCall> toolCalls(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            List<AssistantMessage.ToolCall> result = new ArrayList<>();
            collectToolCalls(root.path("output"), result);
            return result;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void collectToolCalls(JsonNode node, List<AssistantMessage.ToolCall> result) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectToolCalls(item, result);
            }
            return;
        }
        if ("function_call".equals(node.path("type").asText())) {
            String callId = text(node, "call_id", text(node, "id", ""));
            String name = text(node, "name", "");
            String arguments = text(node, "arguments", OBJECT_MAPPER.createObjectNode().toString());
            if (!callId.isBlank() && !name.isBlank()) {
                result.add(new AssistantMessage.ToolCall(callId, "function", name, arguments));
            }
        }
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText();
    }
}
