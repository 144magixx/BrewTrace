package com.minyuwei.xhs.coffeeagent.agent.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt.PromptTemplateLoader;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelMessageOutputSchemaV2Test {
    @Test
    void loadsParseableStrictSchemaWithFactUpdateContract() {
        JsonNode format = new PromptTemplateLoader().loadJson("prompts/agent/model-message-output-schema-v2.json");
        JsonNode schema = format.path("schema");

        assertTrue(format.path("strict").asBoolean());
        assertFalse(schema.path("additionalProperties").asBoolean());
        assertEquals(propertyNames(schema), requiredNames(schema));
        assertTrue(schema.path("properties").has("factUpdates"));
        assertEquals(20, schema.path("properties").path("factUpdates").path("maxItems").asInt());
        JsonNode item = schema.path("properties").path("factUpdates").path("items");
        assertFalse(item.path("additionalProperties").asBoolean());
        assertEquals(propertyNames(item), requiredNames(item));
    }

    private Set<String> propertyNames(JsonNode schema) {
        Set<String> names = new HashSet<>();
        schema.path("properties").fieldNames().forEachRemaining(names::add);
        return names;
    }

    private Set<String> requiredNames(JsonNode schema) {
        Set<String> names = new HashSet<>();
        schema.path("required").forEach(node -> names.add(node.asText()));
        return names;
    }
}

