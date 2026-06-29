package com.minyuwei.xhs.coffeeagent.memory.application;

import com.minyuwei.xhs.coffeeagent.memory.domain.MemoryRecall;
import com.minyuwei.xhs.coffeeagent.memory.infrastructure.EmbeddingModelGateway;
import com.minyuwei.xhs.coffeeagent.memory.infrastructure.MemoryEmbeddingJdbcRepository;

import java.util.ArrayList;
import java.util.List;

public class MemoryRecallService {
    private final EmbeddingModelGateway embeddingModelGateway;
    private final MemoryEmbeddingJdbcRepository repository;

    public MemoryRecallService(EmbeddingModelGateway embeddingModelGateway, MemoryEmbeddingJdbcRepository repository) {
        this.embeddingModelGateway = embeddingModelGateway;
        this.repository = repository;
    }

    public List<MemoryRecall> recall(String sessionId, String query, int limit) {
        List<Double> queryVector = embeddingModelGateway.embed(query);
        return repository.search(queryVector, limit).stream()
                .map(scored -> MemoryRecall.fromEmbedding(sessionId, query, scored.embedding(), scored.score(), matchedReasons(query, scored.embedding().contentSummary())))
                .toList();
    }

    private List<String> matchedReasons(String query, String summary) {
        List<String> reasons = new ArrayList<>();
        if (query.contains("甜橙") && summary.contains("甜橙")) reasons.add("相同风味关键词：甜橙");
        if (query.contains("红茶") && summary.contains("红茶")) reasons.add("相同风味关键词：红茶");
        if (query.contains("水洗") && summary.contains("水洗")) reasons.add("相同处理法：水洗");
        if (reasons.isEmpty()) reasons.add("语义向量相似");
        return reasons;
    }
}
