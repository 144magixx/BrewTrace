package com.minyuwei.xhs.coffeeagent.shared.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class SensitiveValueRedactor {
    private static final String REDACTED = "[REDACTED]";
    private static final Pattern[] PATTERNS = new Pattern[]{
            Pattern.compile("sk-[A-Za-z0-9_-]{20,}"),
            Pattern.compile("(?i)(Authorization\\s*[:=]\\s*Bearer\\s+)[A-Za-z0-9._-]+"),
            Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9._-]{12,}"),
            Pattern.compile("(?i)(Cookie\\s*[:=]\\s*)[^\\n,;}\"]+"),
            Pattern.compile("(?i)(Session[-_ ]?Token\\s*[:=]\\s*)[A-Za-z0-9._-]+"),
            Pattern.compile("(?i)(OPENAI_API_KEY\\s*=\\s*)[^\\s\\n]+")
    };

    private SensitiveValueRedactor() {
    }

    public static String redact(String value) {
        if (value == null || value.isBlank()) {
            return value == null ? "" : value;
        }
        String result = value;
        result = PATTERNS[0].matcher(result).replaceAll(REDACTED);
        for (int i = 1; i < PATTERNS.length; i++) {
            result = PATTERNS[i].matcher(result).replaceAll("$1" + REDACTED);
        }
        return result;
    }

    public static Map<String, Object> redact(Map<String, ?> values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String text) {
                result.put(entry.getKey(), redact(text));
            } else if (value instanceof Map<?, ?> map) {
                Map<String, Object> nested = new LinkedHashMap<>();
                for (Map.Entry<?, ?> nestedEntry : map.entrySet()) {
                    nested.put(String.valueOf(nestedEntry.getKey()), nestedEntry.getValue());
                }
                result.put(entry.getKey(), redact(nested));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    public static boolean changed(String original) {
        return original != null && !original.equals(redact(original));
    }
}
