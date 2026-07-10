package com.minyuwei.xhs.coffeeagent.flavor.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionGenerator;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlavorSuggestionModelRequestFactory {
    static final String SYSTEM_PROMPT_RESOURCE = "prompts/flavor/model-flavor-suggestion-system-v1.md";
    static final String USER_TASK_RESOURCE = "prompts/flavor/model-flavor-suggestion-user-task-v1.md";
    static final String OUTPUT_SCHEMA_RESOURCE = "prompts/flavor/model-flavor-suggestion-output-schema-v1.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PromptTemplateLoader resourceLoader;

    public FlavorSuggestionModelRequestFactory(PromptTemplateLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Prompt createPrompt(FlavorSuggestionGenerator.GenerationRequest request) {
        String userTask = resourceLoader.render(USER_TASK_RESOURCE, Map.of(
                "inputTerm", request.inputTerm(),
                "temperatureStage", request.temperatureStage().name(),
                "senseType", request.senseType().name()
        ));
        return new Prompt(List.of(
                new SystemMessage(resourceLoader.load(SYSTEM_PROMPT_RESOURCE)),
                new UserMessage(userTask)
        ));
    }

    public String createBody(String modelName, Prompt prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("instructions", prompt.getSystemMessage().getText());
        body.put("reasoning", Map.of("effort", "low"));
        body.put("max_output_tokens", 1000);
        body.put("input", List.of(Map.of(
                "role", "user",
                "content", List.of(Map.of(
                        "type", "input_text",
                        "text", prompt.getUserMessage().getText()
                ))
        )));
        body.put("text", Map.of("format", resourceLoader.loadJson(OUTPUT_SCHEMA_RESOURCE)));
        return toJson(body);
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("风味联想模型请求序列化失败", exception);
        }
    }
}
