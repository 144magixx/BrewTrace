package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.application.ConversationModelMessage;
import com.minyuwei.xhs.coffeeagent.agent.application.CopyVariant;
import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelAgentMessage;
import com.minyuwei.xhs.coffeeagent.agent.application.ModelMessageType;
import com.minyuwei.xhs.coffeeagent.agent.application.PostModelMessage;
import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OpenAiResponsesParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ModelAgentMessage parseMessage(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode payload = root.has("messageType") ? root : extractStructuredPayload(root);
            ModelMessageType messageType = ModelMessageType.valueOf(text(payload, "messageType"));
            ModelAgentMessage message = new ModelAgentMessage(
                    messageType,
                    text(payload, "talk"),
                    parsePost(payload.path("post")),
                    parseConversation(payload.path("conversation")),
                    factUpdates(payload.path("factUpdates")),
                    strings(payload.path("warnings")),
                    null
            );
            if (!message.validationErrors().isEmpty()) {
                throw invalid();
            }
            return message;
        } catch (ModelGatewayException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    public List<CopyVariant> parseVariants(String responseBody) {
        return parseMessage(responseBody).variants();
    }

    private JsonNode extractStructuredPayload(JsonNode root) {
        List<String> candidateTexts = new ArrayList<>();
        collectTextCandidates(root, candidateTexts);
        for (String candidate : candidateTexts) {
            String trimmed = candidate.trim();
            if (!trimmed.startsWith("{")) {
                continue;
            }
            try {
                JsonNode parsed = OBJECT_MAPPER.readTree(trimmed);
                if (parsed.has("messageType")) {
                    return parsed;
                }
            } catch (Exception ignored) {
                // Try the next text segment.
            }
        }
        throw invalid();
    }

    private PostModelMessage parsePost(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        JsonNode variantsNode = node.path("variants");
        if (!variantsNode.isArray()) {
            throw invalid();
        }
        List<CopyVariant> variants = new ArrayList<>();
        for (JsonNode item : variantsNode) {
            variants.add(parseVariant(item));
        }
        return new PostModelMessage(variants, strings(node.path("warnings")));
    }

    private ConversationModelMessage parseConversation(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        return new ConversationModelMessage(
                strings(node.path("questions")),
                answerOptions(node.path("answerOptions")),
                usages(node.path("pendingConfirmations")),
                strings(node.path("warnings"))
        );
    }

    private void collectTextCandidates(JsonNode node, List<String> result) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            result.add(node.asText());
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                collectTextCandidates(item, result);
            }
            return;
        }
        Iterator<JsonNode> children = node.elements();
        while (children.hasNext()) {
            collectTextCandidates(children.next(), result);
        }
    }

    private CopyVariant parseVariant(JsonNode node) {
        CopyVariant.Style style = CopyVariant.Style.valueOf(text(node, "style"));
        return new CopyVariant(
                style,
                style.label(),
                text(node, "title"),
                text(node, "body"),
                strings(node.path("tags")),
                usages(node.path("factUsages")),
                usages(node.path("inferences")),
                usages(node.path("pendingConfirmations")),
                strings(node.path("warnings"))
        );
    }

    private List<CopyVariant.FactUsage> usages(JsonNode array) {
        if (!array.isArray()) {
            return List.of();
        }
        List<CopyVariant.FactUsage> usages = new ArrayList<>();
        for (JsonNode item : array) {
            usages.add(new CopyVariant.FactUsage(
                    textOrDefault(item, "expression", ""),
                    textOrDefault(item, "basisType", "UNSUPPORTED"),
                    textOrDefault(item, "sourceReference", "模型返回未提供来源"),
                    textOrDefault(item, "sourceId", ""),
                    textOrDefault(item, "confidenceLabel", "")
            ));
        }
        return usages;
    }

    /**
     * 将模型返回的类型化事实状态增量解析为应用层契约；缺失字段由后续验证拒绝。
     *
     * @param array 模型响应中的 factUpdates 数组
     * @return 解析后的事实状态增量，缺失数组时为空
     */
    private List<FactUpdate> factUpdates(JsonNode array) {
        if (!array.isArray()) {
            return List.of();
        }
        List<FactUpdate> updates = new ArrayList<>();
        for (JsonNode item : array) {
            updates.add(new FactUpdate(
                    enumValue(FactUpdate.Action.class, item, "action"),
                    enumValue(FactUpdate.Boundary.class, item, "boundary"),
                    textOrDefault(item, "value", ""),
                    textOrDefault(item, "sourceMessageId", ""),
                    textOrDefault(item, "sourceQuote", ""),
                    textOrDefault(item, "reason", ""),
                    nullableText(item, "targetItemId")
            ));
        }
        return updates;
    }

    /**
     * 解析指定枚举字段并将非法枚举统一映射为模型格式错误。
     *
     * @param enumType 目标枚举类型
     * @param node 包含枚举字段的 JSON 节点
     * @param field 字段名
     * @param <E> 枚举类型
     * @return 解析后的枚举值
     */
    private <E extends Enum<E>> E enumValue(Class<E> enumType, JsonNode node, String field) {
        try {
            return Enum.valueOf(enumType, text(node, field));
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    /**
     * 读取允许为 JSON null 的文本字段。
     *
     * @param node 包含字段的 JSON 节点
     * @param field 字段名
     * @return null 节点返回 {@code null}，否则返回文本值
     */
    private String nullableText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private List<ConversationModelMessage.AnswerOption> answerOptions(JsonNode array) {
        if (!array.isArray()) {
            return List.of();
        }
        List<ConversationModelMessage.AnswerOption> options = new ArrayList<>();
        for (JsonNode item : array) {
            options.add(new ConversationModelMessage.AnswerOption(
                    textOrDefault(item, "id", ""),
                    textOrDefault(item, "label", ""),
                    textOrDefault(item, "content", "")
            ));
        }
        return options;
    }

    private List<String> strings(JsonNode array) {
        if (!array.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            values.add(item.asText());
        }
        return values;
    }

    private String text(JsonNode node, String field) {
        String value = textOrDefault(node, field, "");
        if (value.isBlank()) {
            throw invalid();
        }
        return value;
    }

    private String textOrDefault(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText();
    }

    private ModelGatewayException invalid() {
        return new ModelGatewayException(RecoverableModelError.Code.MODEL_FORMAT_INVALID, "模型返回格式异常，未能解析出合法消息路由。");
    }
}
