package com.minyuwei.xhs.coffeeagent.agent.infrastructure.prompt;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptResourceGovernanceTest {
    private static final Pattern JSON_TEXT_BLOCK = Pattern.compile("\"\"\"\\s*[\\[{]", Pattern.DOTALL);
    private static final Pattern ESCAPED_JSON_LITERAL = Pattern.compile("\"\\s*\\{\\\\\"");

    private final PromptTemplateLoader loader = new PromptTemplateLoader();

    @Test
    void allPromptJsonResourcesAreParseable() throws IOException {
        List<Path> roots = List.of(
                Path.of("src/main/resources/prompts"),
                Path.of("src/test/resources/prompts")
        );
        List<String> failures = new ArrayList<>();

        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> parseResource(path, failures));
            }
        }

        assertTrue(failures.isEmpty(), () -> "以下 prompt JSON 资源无法解析：" + failures);
    }

    @Test
    void sourceCodeContainsNoEmbeddedJsonDocumentsOrLiteralModelMessages() throws IOException {
        List<Path> roots = List.of(
                Path.of("src/main/java"),
                Path.of("src/test/java"),
                Path.of("../frontend/src")
        );
        List<String> violations = new ArrayList<>();

        for (Path root : roots) {
            if (!Files.exists(root)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(root)) {
                files.filter(this::isSourceFile)
                        .forEach(path -> inspectSource(path, violations));
            }
        }

        assertTrue(violations.isEmpty(), () -> "发现硬编码 prompt/JSON，请迁移到 resources/prompts：" + violations);
    }

    private void parseResource(Path path, List<String> failures) {
        String resourcePath = path.toString().substring(path.toString().indexOf("resources/") + "resources/".length());
        try {
            loader.loadJson(resourcePath);
        } catch (RuntimeException exception) {
            failures.add(resourcePath + " -> " + exception.getMessage());
        }
    }

    private void inspectSource(Path path, List<String> violations) {
        try {
            String source = Files.readString(path);
            if (JSON_TEXT_BLOCK.matcher(source).find()) {
                violations.add(path + " 包含 JSON 文本块");
            }
            if (ESCAPED_JSON_LITERAL.matcher(source).find()) {
                violations.add(path + " 包含转义 JSON 字符串");
            }
            if (source.contains("new SystemMessage(\"") || source.contains("new UserMessage(\"")) {
                violations.add(path + " 直接构造模型消息文本");
            }
        } catch (IOException exception) {
            violations.add(path + " 读取失败：" + exception.getMessage());
        }
    }

    private boolean isSourceFile(Path path) {
        String fileName = path.toString();
        return fileName.endsWith(".java") || fileName.endsWith(".ts") || fileName.endsWith(".tsx");
    }
}
