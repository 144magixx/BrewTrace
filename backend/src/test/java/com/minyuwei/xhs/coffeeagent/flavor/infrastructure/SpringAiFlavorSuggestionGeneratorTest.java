package com.minyuwei.xhs.coffeeagent.flavor.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.OpenAiResponsesLlmClient;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionGenerator;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAiFlavorSuggestionGeneratorTest {
    private static final String VALID_RESPONSE_RESOURCE = "prompts/fixtures/model-responses/flavor-suggestions-v1.json";
    private static final String INVALID_RESPONSE_RESOURCE = "prompts/fixtures/model-responses/flavor-suggestions-invalid-v1.json";

    private final PromptTemplateLoader resourceLoader = new PromptTemplateLoader();
    private final FlavorSuggestionModelRequestFactory requestFactory = new FlavorSuggestionModelRequestFactory(resourceLoader);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatesStructuredCandidatesThroughDedicatedToolFreeModelChain() throws Exception {
        CapturingResponsesClient client = new CapturingResponsesClient(resourceLoader.load(VALID_RESPONSE_RESOURCE));
        FlavorSuggestionGenerator generator = generator(client);

        List<FlavorSuggestionGenerator.FlavorCandidate> candidates = generator.generate(
                new FlavorSuggestionGenerator.GenerationRequest(
                        "清新的柑橘感",
                        TemperatureFlavor.TemperatureStage.WARM,
                        TemperatureFlavor.SenseType.AROMA
                )
        );

        assertEquals(2, candidates.size());
        assertEquals("佛手柑", candidates.getFirst().name());
        assertEquals("温热阶段的香气仍然清扬。", candidates.getFirst().description());
        assertEquals(1, client.callCount());

        JsonNode requestBody = objectMapper.readTree(client.lastBody());
        assertFalse(requestBody.has("tools"));
        assertFalse(requestBody.has("tool_choice"));
        assertTrue(requestBody.path("text").path("format").path("schema").path("properties").has("suggestions"));
        String requestText = requestBody.toString();
        assertTrue(requestText.contains("清新的柑橘感"));
        assertTrue(requestText.contains("WARM"));
        assertTrue(requestText.contains("AROMA"));
    }

    @Test
    void malformedModelResponseSafelyDegradesToNoCandidates() {
        CapturingResponsesClient client = new CapturingResponsesClient(resourceLoader.load(INVALID_RESPONSE_RESOURCE));
        FlavorSuggestionService service = new FlavorSuggestionService(generator(client));

        assertTrue(service.suggest(
                "s1",
                "坚果感",
                TemperatureFlavor.TemperatureStage.COOL,
                TemperatureFlavor.SenseType.TASTE
        ).isEmpty());
        assertEquals(1, client.callCount());
    }

    @Test
    void loadsVersionedPromptAndParsesStructuredOutputSchema() {
        String systemPrompt = resourceLoader.load(FlavorSuggestionModelRequestFactory.SYSTEM_PROMPT_RESOURCE);
        String renderedTask = resourceLoader.render(
                FlavorSuggestionModelRequestFactory.USER_TASK_RESOURCE,
                java.util.Map.of(
                        "inputTerm", "红茶感",
                        "temperatureStage", "COOL",
                        "senseType", "TASTE"
                )
        );
        JsonNode schema = resourceLoader.loadJson(FlavorSuggestionModelRequestFactory.OUTPUT_SCHEMA_RESOURCE);

        assertTrue(systemPrompt.contains("PENDING_ASSOCIATION"));
        assertTrue(systemPrompt.contains("不得为了凑数量编造"));
        assertTrue(renderedTask.contains("红茶感"));
        assertFalse(renderedTask.contains("{{"));
        assertEquals(8, schema.path("schema").path("properties").path("suggestions").path("maxItems").asInt());
        assertTrue(schema.path("schema").path("properties").path("suggestions").path("items").path("required").toString().contains("reason"));
    }

    private FlavorSuggestionGenerator generator(CapturingResponsesClient client) {
        FlavorSuggestionResponsesChatModel chatModel = new FlavorSuggestionResponsesChatModel(
                client,
                requestFactory,
                "https://example.test/v1",
                "gpt-5.5",
                10,
                "test-key"
        );
        return new SpringAiFlavorSuggestionGenerator(
                ChatClient.builder(chatModel).build(),
                requestFactory,
                new FlavorSuggestionModelResponseParser()
        );
    }

    private static final class CapturingResponsesClient extends OpenAiResponsesLlmClient {
        private final String response;
        private String lastBody;
        private int callCount;

        private CapturingResponsesClient(String response) {
            this.response = response;
        }

        @Override
        public LlmResponse createResponse(String baseUrl, String apiKey, String body, int timeoutSeconds) {
            lastBody = body;
            callCount++;
            return new LlmResponse(200, response);
        }

        private String lastBody() {
            return lastBody;
        }

        private int callCount() {
            return callCount;
        }
    }
}
