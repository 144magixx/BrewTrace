package com.minyuwei.xhs.coffeeagent.memory.infrastructure;

import com.minyuwei.xhs.coffeeagent.memory.domain.MemoryEmbedding;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;

public class MemoryEmbeddingJdbcTest {
    public static void run() {
        MemoryEmbeddingJdbcRepository repository = new MemoryEmbeddingJdbcRepository();
        EmbeddingModelGateway gateway = new EmbeddingModelGateway();
        repository.save(MemoryEmbedding.ofRecord("r1", "咖啡记录：甜橙、红茶、水洗", gateway.embed("甜橙 红茶 水洗")));
        ApiContractTestSupport.assertTrue(repository.count() == 1, "记忆向量必须可写入");
        var results = repository.search(gateway.embed("甜橙 红茶"), 1);
        ApiContractTestSupport.assertTrue(results.getFirst().score() > 0.8, "相似向量必须可召回");
    }
}
