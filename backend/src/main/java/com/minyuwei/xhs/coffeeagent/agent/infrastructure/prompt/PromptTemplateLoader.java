package com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.minyuwei.xhs.coffeeagent.agent.application.RecoverableModelError;
import com.minyuwei.xhs.coffeeagent.agent.infrastructure.ModelGatewayException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PromptTemplateLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public String render(String resourcePath, Map<String, String> variables) {
        String template = load(resourcePath);
        String rendered = template;
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            rendered = rendered.replace("{{" + variable.getKey() + "}}", variable.getValue());
        }
        return rendered;
    }

    public String load(String resourcePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new ModelGatewayException(RecoverableModelError.Code.MODEL_FORMAT_INVALID, "提示词模板不存在：" + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ModelGatewayException(RecoverableModelError.Code.MODEL_FORMAT_INVALID, "提示词模板读取失败：" + resourcePath);
        }
    }

    public JsonNode loadJson(String resourcePath) {
        try {
            return OBJECT_MAPPER.readTree(load(resourcePath));
        } catch (IOException exception) {
            throw new ModelGatewayException(RecoverableModelError.Code.MODEL_FORMAT_INVALID, "JSON 资源格式无效：" + resourcePath);
        }
    }
}
