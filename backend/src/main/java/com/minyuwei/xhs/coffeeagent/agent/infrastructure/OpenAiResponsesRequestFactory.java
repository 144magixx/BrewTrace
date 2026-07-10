package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptBundle;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptComposer;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import com.minyuwei.xhs.coffeeagent.shared.error.SensitiveValueRedactor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenAiResponsesRequestFactory {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String COPY_TASK_TEMPLATE = "prompts/agent/openai-responses-copy-task-v1.md";
    private static final String MODEL_MESSAGE_OUTPUT_SCHEMA = "prompts/agent/model-message-output-schema-v1.json";
    private static final String EMPTY_TOOL_INPUT_SCHEMA = "prompts/tools/common/empty-input-schema-v1.json";
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
        return createBody(modelName, prompt, List.of());
    }

    public String createBody(String modelName, Prompt prompt, List<ToolCallback> fallbackToolCallbacks) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("instructions", systemText(prompt));
        body.put("reasoning", Map.of("effort", "low"));
        body.put("max_output_tokens", 1800);
        body.put("input", input(prompt));
        List<Map<String, Object>> tools = tools(prompt, fallbackToolCallbacks);
        if (!tools.isEmpty()) {
            body.put("tools", tools);
            body.put("tool_choice", "auto");
        }
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

    private List<Map<String, Object>> input(Prompt prompt) {
        List<Map<String, Object>> input = new java.util.ArrayList<>();
        for (Message message : prompt.getInstructions()) {
            if (message.getMessageType() == MessageType.SYSTEM) {
                continue;
            }
            if (message instanceof ToolResponseMessage toolResponseMessage) {
                input.addAll(toolResponseMessage.getResponses().stream()
                        .map(response -> mapOf(
                                "type", "function_call_output",
                                "call_id", response.id(),
                                "output", response.responseData()
                        ))
                        .toList());
                continue;
            }
            if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
                input.addAll(assistantMessage.getToolCalls().stream()
                        .map(toolCall -> mapOf(
                                "type", "function_call",
                                "call_id", toolCall.id(),
                                "name", toolCall.name(),
                                "arguments", toolCall.arguments()
                        ))
                        .toList());
                continue;
            }
            input.add(Map.of(
                    "role", message.getMessageType().getValue(),
                    "content", List.of(Map.of(
                            "type", message.getMessageType() == MessageType.ASSISTANT ? "output_text" : "input_text",
                            "text", message.getText()
                    ))
            ));
        }
        return input.isEmpty() ? List.of(Map.of(
                "role", "user",
                "content", List.of(Map.of("type", "input_text", "text", userText(prompt)))
        )) : input;
    }

    private Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> value = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            value.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return value;
    }

    private List<Map<String, Object>> tools(Prompt prompt, List<ToolCallback> fallbackToolCallbacks) {
        List<ToolCallback> callbacks = fallbackToolCallbacks == null ? List.of() : fallbackToolCallbacks;
        if (prompt.getOptions() instanceof ToolCallingChatOptions options && !options.getToolCallbacks().isEmpty()) {
            callbacks = options.getToolCallbacks();
        }
        return callbacks.stream()
                .map(this::tool)
                .toList();
    }

    private Map<String, Object> tool(ToolCallback callback) {
        org.springframework.ai.tool.definition.ToolDefinition definition = callback.getToolDefinition();
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("name", definition.name());
        tool.put("description", definition.description());
        tool.put("parameters", parseSchema(definition.inputSchema()));
        tool.put("strict", true);
        return tool;
    }

    private JsonNode parseSchema(String schema) {
        if (schema == null || schema.isBlank()) {
            return promptTemplateLoader.loadJson(EMPTY_TOOL_INPUT_SCHEMA);
        }
        try {
            return OBJECT_MAPPER.readTree(schema);
        } catch (JsonProcessingException exception) {
            throw new ModelGatewayException(com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError.Code.MODEL_FORMAT_INVALID, "工具 JSON Schema 构造失败");
        }
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

    private JsonNode schema() {
        return promptTemplateLoader.loadJson(MODEL_MESSAGE_OUTPUT_SCHEMA);
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ModelGatewayException(com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError.Code.MODEL_FORMAT_INVALID, "模型请求 JSON 构造失败");
        }
    }
}
