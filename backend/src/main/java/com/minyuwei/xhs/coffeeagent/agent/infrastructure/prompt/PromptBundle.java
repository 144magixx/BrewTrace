package com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt;

import java.time.Instant;
import java.util.Map;

public record PromptBundle(
        String instructions,
        String baseTemplateVersion,
        String routingRulesVersion,
        Map<String, String> stylePromptVersions,
        String fieldDefinitions,
        String dynamicConstraints,
        Instant createdAt
) {
    public String versionSummary() {
        return "base=" + baseTemplateVersion
                + "; routing=" + routingRulesVersion
                + "; styles=" + stylePromptVersions;
    }
}
