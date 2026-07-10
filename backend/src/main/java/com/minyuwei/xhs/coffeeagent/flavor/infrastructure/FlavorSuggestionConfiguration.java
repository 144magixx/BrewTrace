package com.minyuwei.xhs.coffeeagent.flavor.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiResponsesLlmClient;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionGenerator;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.shared.config.ModelProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlavorSuggestionConfiguration {
    @Bean
    public FlavorSuggestionModelRequestFactory flavorSuggestionModelRequestFactory(PromptTemplateLoader resourceLoader) {
        return new FlavorSuggestionModelRequestFactory(resourceLoader);
    }

    @Bean
    public FlavorSuggestionModelResponseParser flavorSuggestionModelResponseParser() {
        return new FlavorSuggestionModelResponseParser();
    }

    @Bean
    public FlavorSuggestionGenerator flavorSuggestionGenerator(
            OpenAiResponsesLlmClient client,
            FlavorSuggestionModelRequestFactory requestFactory,
            FlavorSuggestionModelResponseParser responseParser,
            ModelProperties modelProperties
    ) {
        ChatModel dedicatedModel = new FlavorSuggestionResponsesChatModel(
                client,
                requestFactory,
                modelProperties.baseUrl(),
                modelProperties.textModel(),
                modelProperties.timeoutSeconds(),
                modelProperties.apiKey()
        );
        ChatClient dedicatedChatClient = ChatClient.builder(dedicatedModel).build();
        return new SpringAiFlavorSuggestionGenerator(dedicatedChatClient, requestFactory, responseParser);
    }

    @Bean
    public FlavorSuggestionService flavorSuggestionService(FlavorSuggestionGenerator generator) {
        return new FlavorSuggestionService(generator);
    }
}
