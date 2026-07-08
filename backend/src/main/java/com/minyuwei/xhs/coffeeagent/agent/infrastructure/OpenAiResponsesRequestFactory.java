package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptBundle;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptComposer;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.shared.error.SensitiveValueRedactor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiResponsesRequestFactory {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String COPY_TASK_TEMPLATE = "prompts/agent/openai-responses-copy-task-v1.md";
    private final PromptComposer promptComposer;
    private final PromptTemplateLoader promptTemplateLoader;

    public OpenAiResponsesRequestFactory(PromptTemplateLoader promptTemplateLoader) {
        this(new PromptComposer(promptTemplateLoader), promptTemplateLoader);
    }

    public OpenAiResponsesRequestFactory(PromptComposer promptComposer, PromptTemplateLoader promptTemplateLoader) {
        this.promptComposer = promptComposer;
        this.promptTemplateLoader = promptTemplateLoader;
    }

    public String createBody(String modelName, ModelContextPackage contextPackage) {
        return createBody(modelName, createPrompt(contextPackage));
    }

    public Prompt createPrompt(ModelContextPackage contextPackage) {
        PromptBundle promptBundle = promptComposer.compose(contextPackage);
        return new Prompt(List.of(
                new SystemMessage(promptBundle.instructions()),
                new UserMessage(userInput(contextPackage, promptBundle))
        ));
    }

    public String createBody(String modelName, Prompt prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("instructions", systemText(prompt));
        body.put("reasoning", Map.of("effort", "low"));
        body.put("max_output_tokens", 1800);
        body.put("input", List.of(Map.of(
                "role", "user",
                "content", List.of(Map.of(
                        "type", "input_text",
                        "text", userText(prompt)
                ))
        )));
        body.put("text", Map.of("format", schema()));
        return toJson(body);
    }

    public String createPreviewBody(String modelName, ModelContextPackage contextPackage) {
        return SensitiveValueRedactor.redact(createBody(modelName, contextPackage));
    }

    public String createPreviewBody(String modelName, Prompt prompt) {
        return SensitiveValueRedactor.redact(createBody(modelName, prompt));
    }

    private String systemText(Prompt prompt) {
        return prompt.getSystemMessage() == null ? "" : prompt.getSystemMessage().getText();
    }

    private String userText(Prompt prompt) {
        return prompt.getUserMessage() == null ? prompt.getContents() : prompt.getUserMessage().getText();
    }

    private String userInput(ModelContextPackage contextPackage, PromptBundle promptBundle) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", contextPackage.sessionId());
        payload.put("currentSession", contextPackage.currentSession());
        payload.put("confirmedFacts", contextPackage.confirmedFacts());
        payload.put("pendingAssociations", contextPackage.pendingAssociations());
        payload.put("candidateMemoryBoundaries", contextPackage.candidateMemoryBoundaries());
        payload.put("excludedItems", contextPackage.excludedItems());
        payload.put("task", promptTemplateLoader.render(COPY_TASK_TEMPLATE, Map.of()));
        payload.put("promptBundle", Map.of(
                "baseTemplateVersion", promptBundle.baseTemplateVersion(),
                "routingRulesVersion", promptBundle.routingRulesVersion(),
                "stylePromptVersions", promptBundle.stylePromptVersions(),
                "fieldDefinitions", promptBundle.fieldDefinitions()
        ));
        return toJson(payload);
    }

    private Map<String, Object> schema() {
        Map<String, Object> variant = new LinkedHashMap<>();
        variant.put("type", "object");
        variant.put("additionalProperties", false);
        variant.put("required", List.of("style", "styleLabel", "title", "body", "tags", "factUsages", "inferences", "pendingConfirmations", "warnings"));
        variant.put("properties", Map.of(
                "style", Map.of("type", "string", "enum", List.of("RESTRAINED", "EXAGGERATED", "SHARP_REVIEW")),
                "styleLabel", Map.of("type", "string"),
                "title", Map.of("type", "string"),
                "body", Map.of("type", "string"),
                "tags", Map.of("type", "array", "items", Map.of("type", "string")),
                "factUsages", usageArraySchema(),
                "inferences", usageArraySchema(),
                "pendingConfirmations", usageArraySchema(),
                "warnings", Map.of("type", "array", "items", Map.of("type", "string"))
        ));
        Map<String, Object> conversation = new LinkedHashMap<>();
        conversation.put("type", List.of("object", "null"));
        conversation.put("additionalProperties", false);
        conversation.put("required", List.of("questions", "answerOptions", "pendingConfirmations", "warnings"));
        conversation.put("properties", Map.of(
                "questions", Map.of("type", "array", "minItems", 1, "maxItems", 1, "items", Map.of("type", "string")),
                "answerOptions", Map.of(
                        "type", "array",
                        "maxItems", 4,
                        "items", answerOptionSchema()
                ),
                "pendingConfirmations", usageArraySchema(),
                "warnings", Map.of("type", "array", "items", Map.of("type", "string"))
        ));
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("type", List.of("object", "null"));
        post.put("additionalProperties", false);
        post.put("required", List.of("variants", "warnings"));
        post.put("properties", Map.of(
                "variants", Map.of(
                        "type", "array",
                        "minItems", 3,
                        "maxItems", 3,
                        "items", variant
                ),
                "warnings", Map.of("type", "array", "items", Map.of("type", "string"))
        ));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("messageType", "talk", "conversation", "post", "warnings"));
        schema.put("properties", Map.of(
                "messageType", Map.of("type", "string", "enum", List.of("CONVERSATION", "POST")),
                "talk", Map.of("type", "string"),
                "conversation", conversation,
                "post", post,
                "warnings", Map.of("type", "array", "items", Map.of("type", "string"))
        ));
        return Map.of(
                "type", "json_schema",
                "name", "coffee_model_message",
                "strict", true,
                "schema", schema
        );
    }

    private Map<String, Object> usageArraySchema() {
        return Map.of(
                "type", "array",
                "items", Map.of(
                        "type", "object",
                        "additionalProperties", false,
                        "required", List.of("expression", "basisType", "sourceReference", "sourceId", "confidenceLabel"),
                        "properties", Map.of(
                                "expression", Map.of("type", "string"),
                                "basisType", Map.of("type", "string"),
                                "sourceReference", Map.of("type", "string"),
                                "sourceId", Map.of("type", "string"),
                                "confidenceLabel", Map.of("type", "string")
                        )
                )
        );
    }

    private Map<String, Object> answerOptionSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("id", "label", "content"),
                "properties", Map.of(
                        "id", Map.of("type", "string"),
                        "label", Map.of("type", "string"),
                        "content", Map.of("type", "string")
                )
        );
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ModelGatewayException(com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError.Code.MODEL_FORMAT_INVALID, "模型请求 JSON 构造失败");
        }
    }
}
