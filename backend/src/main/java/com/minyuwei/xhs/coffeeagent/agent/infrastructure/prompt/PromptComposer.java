package com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class PromptComposer {
    private static final String BASE_PROMPT_TEMPLATE = "prompts/agent/openai-responses-copy-v1.md";
    private static final String FACT_STATE_UPDATES = "prompts/agent/fact-state-updates-v1.md";
    private static final String RESTRAINED_STYLE_PROMPT = "prompts/style/restrained-style-v1.md";
    private static final String EXAGGERATED_STYLE_PROMPT = "prompts/style/exaggerated-style-v1.md";
    private static final String SHARP_REVIEW_STYLE_PROMPT = "prompts/style/sharp-review-style-v1.md";
    private static final String FIELD_DEFINITIONS = "prompts/agent/model-message-field-definitions-v2.md";

    private final PromptTemplateLoader loader;

    public PromptComposer(PromptTemplateLoader loader) {
        this.loader = loader;
    }

    public PromptBundle compose(ModelContextPackage contextPackage) {
        String dynamicConstraints = String.join("\n", contextPackage.promptConstraints());
        Map<String, String> styles = new LinkedHashMap<>();
        styles.put("RESTRAINED", loader.load(RESTRAINED_STYLE_PROMPT));
        styles.put("EXAGGERATED", loader.load(EXAGGERATED_STYLE_PROMPT));
        styles.put("SHARP_REVIEW", loader.load(SHARP_REVIEW_STYLE_PROMPT));
        String instructions = loader.render(BASE_PROMPT_TEMPLATE, Map.of(
                "additionalPromptConstraints", dynamicConstraints,
                "factStateUpdateRules", loader.load(FACT_STATE_UPDATES),
                "restrainedStylePrompt", styles.get("RESTRAINED"),
                "exaggeratedStylePrompt", styles.get("EXAGGERATED"),
                "sharpReviewStylePrompt", styles.get("SHARP_REVIEW")
        ));
        return new PromptBundle(
                instructions,
                "openai-responses-copy-v1",
                "model-message-routing-fact-state-v2",
                Map.of(
                        "RESTRAINED", "restrained-style-v1",
                        "EXAGGERATED", "exaggerated-style-v1",
                        "SHARP_REVIEW", "sharp-review-style-v1"
                ),
                loader.load(FIELD_DEFINITIONS).trim(),
                dynamicConstraints,
                Instant.now()
        );
    }
}
