package com.minyuwei.xhs.coffeeagent.flavor.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.ModelGatewayException;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiResponsesLlmClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

public class FlavorSuggestionResponsesChatModel implements ChatModel {
    private final OpenAiResponsesLlmClient client;
    private final FlavorSuggestionModelRequestFactory requestFactory;
    private final String baseUrl;
    private final String modelName;
    private final int timeoutSeconds;
    private final String apiKey;

    public FlavorSuggestionResponsesChatModel(
            OpenAiResponsesLlmClient client,
            FlavorSuggestionModelRequestFactory requestFactory,
            String baseUrl,
            String modelName,
            int timeoutSeconds,
            String apiKey
    ) {
        this.client = client;
        this.requestFactory = requestFactory;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
        this.timeoutSeconds = timeoutSeconds;
        this.apiKey = apiKey;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        String requestBody = requestFactory.createBody(modelName, prompt);
        OpenAiResponsesLlmClient.LlmResponse response = client.createResponse(baseUrl, apiKey, requestBody, timeoutSeconds);
        return new ChatResponse(List.of(new Generation(new AssistantMessage(response.body()))));
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.error(new ModelGatewayException(
                RecoverableModelError.Code.MODEL_SERVICE_UNAVAILABLE,
                "风味联想模型暂不支持流式返回。"
        ));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptions.builder().model(modelName).build();
    }

    @Override
    public ChatOptions getOptions() {
        return getDefaultOptions();
    }
}
