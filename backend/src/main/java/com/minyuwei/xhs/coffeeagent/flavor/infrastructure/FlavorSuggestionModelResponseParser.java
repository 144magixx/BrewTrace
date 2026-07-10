package com.minyuwei.xhs.coffeeagent.flavor.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionGenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FlavorSuggestionModelResponseParser {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public List<FlavorSuggestionGenerator.FlavorCandidate> parse(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode payload = root.has("suggestions") ? root : extractStructuredPayload(root);
            JsonNode suggestions = payload.path("suggestions");
            if (!suggestions.isArray()) {
                throw invalid();
            }
            List<FlavorSuggestionGenerator.FlavorCandidate> result = new ArrayList<>();
            for (JsonNode item : suggestions) {
                result.add(new FlavorSuggestionGenerator.FlavorCandidate(
                        text(item, "name"),
                        text(item, "description"),
                        text(item, "reason")
                ));
            }
            return result;
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid();
        }
    }

    private JsonNode extractStructuredPayload(JsonNode root) {
        List<String> candidates = new ArrayList<>();
        collectTextCandidates(root, candidates);
        for (String candidate : candidates) {
            String trimmed = candidate.trim();
            if (!trimmed.startsWith("{")) {
                continue;
            }
            try {
                JsonNode parsed = OBJECT_MAPPER.readTree(trimmed);
                if (parsed.has("suggestions")) {
                    return parsed;
                }
            } catch (Exception ignored) {
                // Continue with the next structured text segment.
            }
        }
        throw invalid();
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

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText();
    }

    private IllegalStateException invalid() {
        return new IllegalStateException("风味联想模型返回格式异常");
    }
}
