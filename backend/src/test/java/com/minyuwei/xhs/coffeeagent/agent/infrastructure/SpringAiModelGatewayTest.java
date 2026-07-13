package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMessageType;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMode;
import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor.AgentTraceAdvisor;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor.ContextPreviewAdvisor;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor.FactBoundaryAdvisor;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.fixtures.ModelResponseFixtures;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionGenerator;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.support.FakeFlavorSuggestionGenerator;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallPolicy;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolCallRecorder;
import com.minyuwei.xhs.coffeeagent.tools.application.ToolRegistry;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.FlavorSuggestionToolAdapter;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.FlavorSuggestionToolRegistrar;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.SpringAiToolCallbackAdapter;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceService;
import com.minyuwei.xhs.coffeeagent.trace.infrastructure.AgentTraceRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAiModelGatewayTest {
    private static final String CAPTURED_REQUEST_BODY = new PromptTemplateLoader()
            .load("prompts/fixtures/model-requests/captured-request-v1.json")
            .trim();
    private final OpenAiResponsesRequestFactory requestFactory = new OpenAiResponsesRequestFactory(new PromptTemplateLoader());
    private final OpenAiResponsesParser parser = new OpenAiResponsesParser();

    /**
     * 验证不具备真实 HTTP 发送层的通用 ChatModel 仍能完成会话消息映射，且不会伪造请求预览正文。
     */
    @Test
    void mapsConversationMessageFromSpringAiChatClient() {
        CapturingChatModel chatModel = new CapturingChatModel(ModelResponseFixtures.conversation());
        SpringAiModelGateway gateway = gateway(chatModel);

        var result = gateway.complete(contextPackage());

        assertEquals("REAL_MODEL", result.outputType());
        assertEquals(ModelMessageType.CONVERSATION, result.messageType(), () -> "recoverableError=" + result.recoverableError() + ", responsePreview=" + result.responsePreview().rawJson());
        assertTrue(result.talk().contains("最明显的风味"));
        assertTrue(result.requestPreview().rawJson().isEmpty());
        assertTrue(chatModel.lastPrompt().getContents().contains("currentSession"));
    }

    @Test
    void mapsPostMessageFromSpringAiChatClient() {
        SpringAiModelGateway gateway = gateway(new CapturingChatModel(ModelResponseFixtures.post()));

        var result = gateway.complete(contextPackage());

        assertEquals("REAL_MODEL", result.outputType());
        assertEquals(ModelMessageType.POST, result.messageType());
        assertNotNull(result.post());
        assertEquals(3, result.variants().size());
        assertTrue(result.responsePreview().rawJson().contains("Spring AI ChatClient"));
    }

    @Test
    void mapsInvalidModelPayloadToRecoverableFormatError() {
        SpringAiModelGateway gateway = gateway(new CapturingChatModel(ModelResponseFixtures.invalidMinimal()));

        var result = gateway.complete(contextPackage());

        assertEquals("ERROR", result.outputType());
        assertNotNull(result.recoverableError());
        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, result.recoverableError().code());
    }

    /**
     * 验证 Advisor 链展示发送层捕获的原始请求体，并继续记录模型调用轨迹。
     */
    @Test
    void runsSpringAiAdvisorChainAndKeepsGatewayContract() {
        ActualModelRequestCapture requestCapture = new ActualModelRequestCapture();
        CapturingChatModel chatModel = new CapturingChatModel(ModelResponseFixtures.post(), requestCapture);
        AgentTraceRepositoryAdapter repository = new AgentTraceRepositoryAdapter();
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(List.of(
                new FactBoundaryAdvisor(),
                new ContextPreviewAdvisor(requestCapture),
                new AgentTraceAdvisor(new AgentTraceRecorder(new AgentTraceService(repository)))
        )).build();
        SpringAiModelGateway gateway = new SpringAiModelGateway(chatClient, requestFactory, parser, "gpt-5.5", List.of(), requestCapture);

        var result = gateway.complete(contextPackage());

        assertEquals("REAL_MODEL", result.outputType());
        assertEquals(ModelMessageType.POST, result.messageType());
        assertEquals(CAPTURED_REQUEST_BODY, result.requestPreview().rawJson());
        assertTrue(result.responsePreview().rawJson().contains("advisorTraceRecorded"));
        assertEquals(1, repository.findBySessionId("s1").size());
    }

    /**
     * 验证工具调用循环结束后，请求预览与 HTTP Client 收到的最后一份请求体完全一致。
     */
    @Test
    void executesFlavorSuggestionToolCallThroughSpringAiToolCallback() {
        ToolRegistry registry = new ToolRegistry();
        new FlavorSuggestionToolRegistrar(new PromptTemplateLoader()).register(registry, flavorSuggestionService());
        ToolCallRecorder recorder = new ToolCallRecorder();
        SpringAiToolCallbackAdapter callback = new SpringAiToolCallbackAdapter(
                registry,
                new ToolCallPolicy(),
                recorder,
                FlavorSuggestionToolAdapter.TOOL_NAME
        );
        CapturingResponsesClient client = new CapturingResponsesClient(List.of(
                ModelResponseFixtures.flavorSuggestionToolCall(),
                ModelResponseFixtures.conversation()
        ));
        ActualModelRequestCapture requestCapture = new ActualModelRequestCapture();
        ResponsesApiChatModel chatModel = new ResponsesApiChatModel(client, requestFactory, "https://example.test/v1", "gpt-5.5", 10,
                "test-key", List.of(callback), requestCapture);
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(ToolCallingAdvisor.builder()
                        .toolCallingManager(DefaultToolCallingManager.builder()
                                .toolCallbackResolver(new StaticToolCallbackResolver(List.of(callback)))
                                .build())
                        .build())
                .build();
        SpringAiModelGateway gateway = new SpringAiModelGateway(chatClient, requestFactory, parser, "gpt-5.5", List.of(callback), requestCapture);

        var result = gateway.complete(contextPackage());

        assertEquals(ModelMessageType.CONVERSATION, result.messageType());
        assertEquals(1, recorder.records().size());
        assertEquals("flavor_suggestion", recorder.records().getFirst().toolName());
        assertEquals(2, client.bodies().size());
        assertTrue(client.bodies().get(1).contains("function_call_output"));
        assertTrue(client.bodies().get(1).contains("甜橙"));
        assertEquals(client.bodies().getLast(), result.requestPreview().rawJson());
        assertTrue(result.requestPreview().rawJson().contains("根据用户给出的模糊咖啡风味词"));
    }

    private SpringAiModelGateway gateway(ChatModel chatModel) {
        return new SpringAiModelGateway(ChatClient.create(chatModel), requestFactory, parser, "gpt-5.5");
    }

    private FlavorSuggestionService flavorSuggestionService() {
        return new FlavorSuggestionService(new FakeFlavorSuggestionGenerator(List.of(
                new FlavorSuggestionGenerator.FlavorCandidate("甜橙", "圆润甜感", "由柑橘词联想到甜橙")
        )));
    }

    private ModelContextPackage contextPackage() {
        return new ModelContextPackage(
                "s1",
                ModelMode.OPENAI_GPT55,
                List.of(new ModelContextPackage.ContextEntry("ctx1", "今天喝了一支水洗埃塞", "USER_CONFIRMED", "WILL_SEND", null)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("已确认事实可以进入文案依据。"),
                Instant.parse("2026-06-30T00:00:00Z")
        );
    }

    private static final class CapturingChatModel implements ChatModel {
        private final String responseContent;
        private final AtomicReference<Prompt> lastPrompt = new AtomicReference<>();
        private final ActualModelRequestCapture requestCapture;

        /**
         * 创建仅捕获 Prompt 的测试模型。
         *
         * @param responseContent 固定返回的模型内容
         */
        private CapturingChatModel(String responseContent) {
            this(responseContent, null);
        }

        /**
         * 创建同时模拟发送层实际请求体捕获的测试模型。
         *
         * @param responseContent 固定返回的模型内容
         * @param requestCapture 需要记录实际发送体的共享捕获器
         */
        private CapturingChatModel(String responseContent, ActualModelRequestCapture requestCapture) {
            this.responseContent = responseContent;
            this.requestCapture = requestCapture;
        }

        /**
         * 保存 Prompt，按需记录模拟的实际发送体并返回固定响应。
         *
         * @param prompt 本轮测试 Prompt
         * @return 固定的 ChatResponse
         */
        @Override
        public ChatResponse call(Prompt prompt) {
            lastPrompt.set(prompt);
            if (requestCapture != null) {
                requestCapture.record(CAPTURED_REQUEST_BODY);
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage(responseContent))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return ChatOptions.builder().model("gpt-5.5").build();
        }

        @Override
        public ChatOptions getOptions() {
            return getDefaultOptions();
        }

        private Prompt lastPrompt() {
            return lastPrompt.get();
        }
    }

    private static final class CapturingResponsesClient extends OpenAiResponsesLlmClient {
        private final List<String> responses;
        private final List<String> bodies = new ArrayList<>();
        private int index = 0;

        private CapturingResponsesClient(List<String> responses) {
            this.responses = responses;
        }

        @Override
        public LlmResponse createResponse(String baseUrl, String apiKey, String body, int timeoutSeconds) {
            bodies.add(body);
            return new LlmResponse(200, responses.get(index++));
        }

        private List<String> bodies() {
            return bodies;
        }
    }
}
