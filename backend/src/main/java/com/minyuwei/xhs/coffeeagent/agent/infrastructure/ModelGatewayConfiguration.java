package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptComposer;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor.AgentTraceAdvisor;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor.ContextPreviewAdvisor;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor.FactBoundaryAdvisor;
import com.minyuwei.xhs.coffeeagent.shared.config.ModelProperties;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;

@Configuration
public class ModelGatewayConfiguration {
    @Bean
    public OpenAiResponsesLlmClient openAiResponsesLlmClient() {
        return new OpenAiResponsesLlmClient();
    }

    @Bean
    public PromptTemplateLoader promptTemplateLoader() {
        return new PromptTemplateLoader();
    }

    @Bean
    public PromptComposer promptComposer(PromptTemplateLoader promptTemplateLoader) {
        return new PromptComposer(promptTemplateLoader);
    }

    @Bean
    public OpenAiResponsesRequestFactory openAiResponsesRequestFactory(PromptComposer promptComposer, PromptTemplateLoader promptTemplateLoader) {
        return new OpenAiResponsesRequestFactory(promptComposer, promptTemplateLoader);
    }

    @Bean
    public OpenAiResponsesParser openAiResponsesParser() {
        return new OpenAiResponsesParser();
    }

    @Bean
    @Primary
    public ChatModel responsesApiChatModel(
            OpenAiResponsesLlmClient client,
            OpenAiResponsesRequestFactory requestFactory,
            ModelProperties modelProperties
    ) {
        return new ResponsesApiChatModel(client, requestFactory, modelProperties.baseUrl(), modelProperties.textModel(), modelProperties.timeoutSeconds(), modelProperties.apiKey());
    }

    @Bean
    public FactBoundaryAdvisor factBoundaryAdvisor() {
        return new FactBoundaryAdvisor();
    }

    @Bean
    public ContextPreviewAdvisor contextPreviewAdvisor(OpenAiResponsesRequestFactory requestFactory) {
        return new ContextPreviewAdvisor(requestFactory);
    }

    @Bean
    public AgentTraceAdvisor agentTraceAdvisor(AgentTraceRecorder traceRecorder) {
        return new AgentTraceAdvisor(traceRecorder);
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, List<Advisor> advisors, List<ChatClientBuilderCustomizer> customizers) {
        ChatClient.Builder builder = ChatClient.builder(chatModel).defaultAdvisors(advisors);
        customizers.forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    public ModelGateway modelGateway(ChatClient chatClient, OpenAiResponsesRequestFactory requestFactory, OpenAiResponsesParser parser, ModelProperties modelProperties) {
        return new SpringAiModelGateway(chatClient, requestFactory, parser, modelProperties.textModel());
    }
}
