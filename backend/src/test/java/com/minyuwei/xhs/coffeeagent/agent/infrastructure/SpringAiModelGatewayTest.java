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
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceRecorder;
import com.minyuwei.xhs.coffeeagent.trace.application.AgentTraceService;
import com.minyuwei.xhs.coffeeagent.trace.infrastructure.AgentTraceRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAiModelGatewayTest {
    private final OpenAiResponsesRequestFactory requestFactory = new OpenAiResponsesRequestFactory(new PromptTemplateLoader());
    private final OpenAiResponsesParser parser = new OpenAiResponsesParser();

    @Test
    void mapsConversationMessageFromSpringAiChatClient() {
        CapturingChatModel chatModel = new CapturingChatModel(ModelResponseFixtures.conversation());
        SpringAiModelGateway gateway = gateway(chatModel);

        var result = gateway.complete(contextPackage());

        assertEquals("REAL_MODEL", result.outputType());
        assertEquals(ModelMessageType.CONVERSATION, result.messageType());
        assertTrue(result.talk().contains("最明显的风味"));
        assertTrue(result.requestPreview().label().contains("Spring AI"));
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
        SpringAiModelGateway gateway = gateway(new CapturingChatModel("{\"messageType\":\"POST\"}"));

        var result = gateway.complete(contextPackage());

        assertEquals("ERROR", result.outputType());
        assertNotNull(result.recoverableError());
        assertEquals(RecoverableModelError.Code.MODEL_FORMAT_INVALID, result.recoverableError().code());
    }

    @Test
    void runsSpringAiAdvisorChainAndKeepsGatewayContract() {
        CapturingChatModel chatModel = new CapturingChatModel(ModelResponseFixtures.post());
        AgentTraceRepositoryAdapter repository = new AgentTraceRepositoryAdapter();
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(List.of(
                new FactBoundaryAdvisor(),
                new ContextPreviewAdvisor(requestFactory),
                new AgentTraceAdvisor(new AgentTraceRecorder(new AgentTraceService(repository)))
        )).build();
        SpringAiModelGateway gateway = new SpringAiModelGateway(chatClient, requestFactory, parser, "gpt-5.5");

        var result = gateway.complete(contextPackage());

        assertEquals("REAL_MODEL", result.outputType());
        assertEquals(ModelMessageType.POST, result.messageType());
        assertTrue(result.requestPreview().label().contains("Advisor"));
        assertTrue(result.responsePreview().rawJson().contains("advisorTraceRecorded"));
        assertEquals(1, repository.findBySessionId("s1").size());
    }

    private SpringAiModelGateway gateway(ChatModel chatModel) {
        return new SpringAiModelGateway(ChatClient.create(chatModel), requestFactory, parser, "gpt-5.5");
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

        private CapturingChatModel(String responseContent) {
            this.responseContent = responseContent;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            lastPrompt.set(prompt);
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
}
