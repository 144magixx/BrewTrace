package com.minyuwei.xhs.coffeeagent.support;

public class ApiContractTestSupport {
    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void assertContains(String content, String expected, String message) {
        assertTrue(content != null && content.contains(expected), message + " expected=" + expected + " actual=" + content);
    }
}
