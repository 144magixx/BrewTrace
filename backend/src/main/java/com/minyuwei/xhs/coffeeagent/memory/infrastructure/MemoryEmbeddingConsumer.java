package com.minyuwei.xhs.coffeeagent.memory.infrastructure;

import com.minyuwei.xhs.coffeeagent.memory.domain.MemoryEmbedding;
import com.minyuwei.xhs.coffeeagent.tasting.domain.CoffeeRecord;

public class MemoryEmbeddingConsumer {
    private final EmbeddingModelGateway embeddingModelGateway;
    private final MemoryEmbeddingJdbcRepository repository;

    public MemoryEmbeddingConsumer(EmbeddingModelGateway embeddingModelGateway, MemoryEmbeddingJdbcRepository repository) {
        this.embeddingModelGateway = embeddingModelGateway;
        this.repository = repository;
    }

    public void handle(CoffeeRecord record) {
        repository.save(MemoryEmbedding.ofRecord(record.id(), record.summary(), embeddingModelGateway.embed(record.summary())));
    }
}
