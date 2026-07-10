package com.minyuwei.xhs.coffeeagent.agent.infrastructure.fixtures;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;

import java.util.function.Consumer;

public final class ModelResponseFixtures {
    private static final String CONVERSATION_RESOURCE = "prompts/fixtures/model-responses/conversation-v1.json";
    private static final String POST_RESOURCE = "prompts/fixtures/model-responses/post-v1.json";
    private static final String INVALID_MINIMAL_RESOURCE = "prompts/fixtures/model-responses/invalid-minimal-v1.json";
    private static final String FLAVOR_TOOL_CALL_RESOURCE = "prompts/fixtures/model-responses/flavor-suggestion-tool-call-v1.json";
    private static final PromptTemplateLoader RESOURCE_LOADER = new PromptTemplateLoader();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ModelResponseFixtures() {
    }

    public static String conversation() {
        return RESOURCE_LOADER.load(CONVERSATION_RESOURCE);
    }

    public static String post() {
        return RESOURCE_LOADER.load(POST_RESOURCE);
    }

    public static String invalidMinimal() {
        return RESOURCE_LOADER.load(INVALID_MINIMAL_RESOURCE);
    }

    public static String flavorSuggestionToolCall() {
        return RESOURCE_LOADER.load(FLAVOR_TOOL_CALL_RESOURCE);
    }

    public static String conversationWithoutAnswerOptions() {
        return mutate(conversation(), root -> conversationNode(root).remove("answerOptions"));
    }

    public static String invalidPostMissingStyle() {
        return mutate(post(), root -> variant(root, 2).put("style", "EXAGGERATED"));
    }

    public static String conversationWithoutTalk() {
        return mutate(conversation(), root -> root.remove("talk"));
    }

    public static String conversationCarryingPostDrafts() {
        return mutate(post(), root -> root.put("messageType", "CONVERSATION"));
    }

    public static String illegalMessageType() {
        return mutate(conversation(), root -> root.put("messageType", "PUBLISH"));
    }

    public static String conversationWithTooManyQuestions() {
        return mutate(conversation(), root -> ((ArrayNode) conversationNode(root).path("questions"))
                .add("是否知道产区或处理法？"));
    }

    public static String conversationWithTooManyAnswerOptions() {
        return mutate(conversation(), root -> {
            ArrayNode options = (ArrayNode) conversationNode(root).path("answerOptions");
            addAnswerOption(options, "four", "四", "四");
            addAnswerOption(options, "five", "五", "五");
        });
    }

    private static ObjectNode conversationNode(ObjectNode root) {
        return (ObjectNode) root.path("conversation");
    }

    private static ObjectNode variant(ObjectNode root, int index) {
        return (ObjectNode) root.path("post").path("variants").path(index);
    }

    private static void addAnswerOption(ArrayNode options, String id, String label, String content) {
        ObjectNode option = options.addObject();
        option.put("id", id);
        option.put("label", label);
        option.put("content", content);
    }

    private static String mutate(String source, Consumer<ObjectNode> mutation) {
        try {
            JsonNode parsed = OBJECT_MAPPER.readTree(source);
            ObjectNode root = (ObjectNode) parsed;
            mutation.accept(root);
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("模型响应测试资源处理失败", exception);
        }
    }
}
