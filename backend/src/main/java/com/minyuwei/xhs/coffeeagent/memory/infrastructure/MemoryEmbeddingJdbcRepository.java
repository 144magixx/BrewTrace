package com.minyuwei.xhs.coffeeagent.memory.infrastructure;

import com.minyuwei.xhs.coffeeagent.memory.domain.MemoryEmbedding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MemoryEmbeddingJdbcRepository {
    private final List<MemoryEmbedding> embeddings = new ArrayList<>();

    public void save(MemoryEmbedding embedding) {
        embeddings.removeIf(existing -> existing.ownerId().equals(embedding.ownerId()) && existing.embeddingType() == embedding.embeddingType());
        embeddings.add(embedding);
    }

    public List<ScoredEmbedding> search(List<Double> queryVector, int limit) {
        return embeddings.stream()
                .map(embedding -> new ScoredEmbedding(embedding, cosine(queryVector, embedding.vector())))
                .sorted(Comparator.comparingDouble(ScoredEmbedding::score).reversed())
                .limit(limit)
                .toList();
    }

    public int count() {
        return embeddings.size();
    }

    private double cosine(List<Double> a, List<Double> b) {
        double dot = 0;
        double aa = 0;
        double bb = 0;
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            dot += a.get(i) * b.get(i);
            aa += a.get(i) * a.get(i);
            bb += b.get(i) * b.get(i);
        }
        return dot / (Math.sqrt(aa) * Math.sqrt(bb));
    }

    public record ScoredEmbedding(MemoryEmbedding embedding, double score) {
    }
}
