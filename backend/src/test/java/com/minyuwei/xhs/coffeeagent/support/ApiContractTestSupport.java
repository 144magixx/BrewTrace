package com.minyuwei.xhs.coffeeagent.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiContractTestSupport {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertContains(String content, String expected, String message) {
        assertTrue(content != null && content.contains(expected), message + " expected=" + expected + " actual=" + content);
    }

    public static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("测试请求 JSON 序列化失败", exception);
        }
    }
}
