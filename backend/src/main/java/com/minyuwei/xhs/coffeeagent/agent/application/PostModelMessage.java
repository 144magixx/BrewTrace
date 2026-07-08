package com.minyuwei.xhs.coffeeagent.agent.application;

import java.util.List;

public record PostModelMessage(
        List<CopyVariant> variants,
        List<String> warnings
) {
    public PostModelMessage {
        variants = variants == null ? List.of() : List.copyOf(variants);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
