package com.minyuwei.xhs.coffeeagent.flavor.application;

import com.minyuwei.xhs.coffeeagent.flavor.domain.FlavorSuggestion;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TemperatureFlavor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FlavorSuggestionService {
    private static final int MAX_CANDIDATES = 8;

    private final FlavorSuggestionGenerator generator;

    /**
     * 创建风味建议服务，并使用指定生成器获取候选风味。
     *
     * @param generator 负责根据品鉴输入生成候选风味的生成器
     */
    public FlavorSuggestionService(FlavorSuggestionGenerator generator) {
        this.generator = generator;
    }

    /**
     * 根据用户输入及品鉴维度生成去重、规范化且数量受限的风味建议。
     * 当输入为空、生成失败或没有有效候选项时返回空列表。
     *
     * @param sessionId 当前品鉴会话标识，用于关联生成的建议
     * @param inputTerm 用户输入的风味描述
     * @param stage 当前品鉴的温度阶段
     * @param senseType 当前品鉴使用的感官类型
     * @return 待关联到品鉴记录的风味建议列表，最多包含 {@value #MAX_CANDIDATES} 项
     */
    public List<FlavorSuggestion> suggest(String sessionId, String inputTerm, TemperatureFlavor.TemperatureStage stage, TemperatureFlavor.SenseType senseType) {
        if (inputTerm == null || inputTerm.isBlank()) {
            return List.of();
        }

        List<FlavorSuggestionGenerator.FlavorCandidate> generated;
        try {
            generated = generator.generate(new FlavorSuggestionGenerator.GenerationRequest(inputTerm, stage, senseType));
        } catch (RuntimeException exception) {
            return List.of();
        }
        if (generated == null || generated.isEmpty()) {
            return List.of();
        }

        Map<String, FlavorSuggestionGenerator.FlavorCandidate> uniqueCandidates = new LinkedHashMap<>();
        for (FlavorSuggestionGenerator.FlavorCandidate candidate : generated) {
            FlavorSuggestionGenerator.FlavorCandidate normalized = normalize(candidate);
            if (normalized == null) {
                continue;
            }
            uniqueCandidates.putIfAbsent(normalized.name().toLowerCase(Locale.ROOT), normalized);
            if (uniqueCandidates.size() == MAX_CANDIDATES) {
                break;
            }
        }

        return uniqueCandidates.values().stream()
                .map(candidate -> FlavorSuggestion.pendingAssociation(
                        sessionId,
                        inputTerm,
                        candidate.name(),
                        candidate.description(),
                        stage,
                        senseType,
                        candidate.reason()
                ))
                .toList();
    }

    /**
     * 清理候选风味的文本字段，并过滤字段缺失或内容为空的候选项。
     *
     * @param candidate 待规范化的候选风味，可以为 {@code null}
     * @return 规范化后的候选风味；候选项无效时返回 {@code null}
     */
    private FlavorSuggestionGenerator.FlavorCandidate normalize(FlavorSuggestionGenerator.FlavorCandidate candidate) {
        if (candidate == null) {
            return null;
        }
        String name = trim(candidate.name());
        String description = trim(candidate.description());
        String reason = trim(candidate.reason());
        if (name.isBlank() || description.isBlank() || reason.isBlank()) {
            return null;
        }
        return new FlavorSuggestionGenerator.FlavorCandidate(name, description, reason);
    }

    /**
     * 移除文本首尾空白，并将 {@code null} 统一转换为空字符串。
     *
     * @param value 待清理的文本，可以为 {@code null}
     * @return 非 {@code null} 的清理后文本
     */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
