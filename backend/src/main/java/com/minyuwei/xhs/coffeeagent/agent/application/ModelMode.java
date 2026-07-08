package com.minyuwei.xhs.coffeeagent.agent.application;

public enum ModelMode {
    OPENAI_GPT55("openai-gpt55", "真实模型输出 / GPT-5.5", "gpt-5.5", true);

    private final String code;
    private final String displayName;
    private final String defaultModelName;
    private final boolean requiresApiKey;

    ModelMode(String code, String displayName, String defaultModelName, boolean requiresApiKey) {
        this.code = code;
        this.displayName = displayName;
        this.defaultModelName = defaultModelName;
        this.requiresApiKey = requiresApiKey;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public String defaultModelName() {
        return defaultModelName;
    }

    public boolean requiresApiKey() {
        return requiresApiKey;
    }

    public static ModelMode fromCode(String code) {
        return OPENAI_GPT55;
    }
}
