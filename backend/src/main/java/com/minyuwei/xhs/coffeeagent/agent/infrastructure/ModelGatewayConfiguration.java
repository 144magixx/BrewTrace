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
import org.springframework.ai.tool.ToolCallback;

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

    /**
     * 创建由模型发送层、Advisor 和网关共享的实际请求体捕获器。
     *
     * @return 按调用线程隔离请求体的捕获器
     */
    @Bean
    public ActualModelRequestCapture actualModelRequestCapture() {
        return new ActualModelRequestCapture();
    }

    /**
     * 创建 Responses API ChatModel，并让发送层记录真正交给 HTTP Client 的请求体。
     *
     * @param client Responses API HTTP 客户端
     * @param requestFactory 请求序列化工厂
     * @param modelProperties 当前模型配置
     * @param toolCallbacks 允许模型调用的工具集合
     * @param requestCapture 共享的实际请求体捕获器
     * @return 主模型使用的 ChatModel
     */
    @Bean
    @Primary
    public ChatModel responsesApiChatModel(
            OpenAiResponsesLlmClient client,
            OpenAiResponsesRequestFactory requestFactory,
            ModelProperties modelProperties,
            List<ToolCallback> toolCallbacks,
            ActualModelRequestCapture requestCapture
    ) {
        return new ResponsesApiChatModel(client, requestFactory, modelProperties.baseUrl(), modelProperties.textModel(),
                modelProperties.timeoutSeconds(), modelProperties.apiKey(), toolCallbacks, requestCapture);
    }

    @Bean
    public FactBoundaryAdvisor factBoundaryAdvisor() {
        return new FactBoundaryAdvisor();
    }

    /**
     * 创建请求预览 Advisor，读取发送层已经生成的实际请求体。
     *
     * @param requestCapture 发送层共享的实际请求体捕获器
     * @return 只做脱敏、不重新序列化的请求预览 Advisor
     */
    @Bean
    public ContextPreviewAdvisor contextPreviewAdvisor(ActualModelRequestCapture requestCapture) {
        return new ContextPreviewAdvisor(requestCapture);
    }

    @Bean
    public AgentTraceAdvisor agentTraceAdvisor(AgentTraceRecorder traceRecorder) {
        return new AgentTraceAdvisor(traceRecorder);
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, List<Advisor> advisors, List<ChatClientBuilderCustomizer> customizers) {
        ChatClient.Builder builder = ChatClient.builder(chatModel)
                .defaultAdvisors(advisors);
        customizers.forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    /**
     * 创建业务模型网关，并共享发送层捕获的实际请求体用于成功与失败预览。
     *
     * @param chatClient 执行 Advisor 链和模型调用的客户端
     * @param requestFactory Prompt 构造工厂
     * @param parser 模型结构化响应解析器
     * @param modelProperties 当前模型配置
     * @param toolCallbacks 允许模型调用的工具集合
     * @param requestCapture 发送层共享的实际请求体捕获器
     * @return 业务层使用的模型网关
     */
    @Bean
    public ModelGateway modelGateway(
            ChatClient chatClient,
            OpenAiResponsesRequestFactory requestFactory,
            OpenAiResponsesParser parser,
            ModelProperties modelProperties,
            List<ToolCallback> toolCallbacks,
            ActualModelRequestCapture requestCapture
    ) {
        return new SpringAiModelGateway(chatClient, requestFactory, parser, modelProperties.textModel(), toolCallbacks, requestCapture);
    }
}
