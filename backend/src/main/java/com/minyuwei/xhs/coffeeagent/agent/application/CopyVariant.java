package com.minyuwei.xhs.coffeeagent.agent.application;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CopyVariant(
        Style style,
        String styleLabel,
        String title,
        String body,
        List<String> tags,
        List<FactUsage> factUsages,
        List<FactUsage> inferences,
        List<FactUsage> pendingConfirmations,
        List<String> warnings
) {
    public enum Style {
        RESTRAINED("克制版"),
        EXAGGERATED("夸张版"),
        SHARP_REVIEW("锐评版");

        private final String label;

        Style(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record FactUsage(
            String expression,
            String basisType,
            String sourceReference,
            String sourceId,
            String confidenceLabel
    ) {
    }

    public List<String> validationWarnings() {
        List<String> result = new ArrayList<>();
        if (style == null) {
            result.add("缺少文案风格");
        }
        if (title == null || title.isBlank()) {
            result.add("缺少标题");
        }
        if (body == null || body.isBlank()) {
            result.add("缺少正文");
        }
        for (FactUsage usage : safe(factUsages)) {
            if (!"USER_CONFIRMED".equals(usage.basisType()) && !"CONFIRMED_FACT".equals(usage.basisType())) {
                result.add("事实依据中包含非用户确认内容：" + usage.expression());
            }
        }
        return result;
    }

    public static List<String> validateCompleteSet(List<CopyVariant> variants) {
        List<String> errors = new ArrayList<>();
        if (safe(variants).size() != Style.values().length) {
            errors.add("模型返回必须刚好包含三版文案");
        }
        EnumSet<Style> styles = EnumSet.noneOf(Style.class);
        for (CopyVariant variant : safe(variants)) {
            if (variant.style() != null) {
                styles.add(variant.style());
            }
            errors.addAll(variant.validationWarnings());
        }
        Map<Style, Long> counts = safe(variants).stream()
                .filter(variant -> variant.style() != null)
                .collect(Collectors.groupingBy(CopyVariant::style, Collectors.counting()));
        if (counts.values().stream().anyMatch(count -> count > 1)) {
            errors.add("模型返回包含重复风格");
        }
        if (!styles.containsAll(EnumSet.allOf(Style.class))) {
            errors.add("模型返回未完整包含克制版、夸张版和锐评版");
        }
        return errors;
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
