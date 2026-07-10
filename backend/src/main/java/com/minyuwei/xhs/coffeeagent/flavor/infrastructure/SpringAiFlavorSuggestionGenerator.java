package com.minyuwei.xhs.coffeeagent.flavor.infrastructure;

import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.model.ChatResponse;

import java.util.List;

public class SpringAiFlavorSuggestionGenerator implements FlavorSuggestionGenerator {
    private final ChatClient chatClient;
    private final FlavorSuggestionModelRequestFactory requestFactory;
    private final FlavorSuggestionModelResponseParser responseParser;

    public SpringAiFlavorSuggestionGenerator(
            ChatClient chatClient,
            FlavorSuggestionModelRequestFactory requestFactory,
            FlavorSuggestionModelResponseParser responseParser
    ) {
        this.chatClient = chatClient;
        this.requestFactory = requestFactory;
        this.responseParser = responseParser;
    }

    @Override
    public List<FlavorCandidate> generate(GenerationRequest request) {
        ChatClientResponse clientResponse = chatClient.prompt(requestFactory.createPrompt(request))
                .call()
                .chatClientResponse();
        ChatResponse chatResponse = clientResponse.chatResponse();
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            throw new IllegalStateException("风味联想模型未返回内容");
        }
        return responseParser.parse(chatResponse.getResult().getOutput().getText());
    }
}
