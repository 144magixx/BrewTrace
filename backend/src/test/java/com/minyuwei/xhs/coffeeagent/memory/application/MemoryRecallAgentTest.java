package com.minyuwei.xhs.coffeeagent.memory.application;

import com.minyuwei.xhs.coffeeagent.memory.infrastructure.EmbeddingModelGateway;
import com.minyuwei.xhs.coffeeagent.memory.infrastructure.MemoryEmbeddingConsumer;
import com.minyuwei.xhs.coffeeagent.memory.infrastructure.MemoryEmbeddingJdbcRepository;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tasting.domain.CoffeeRecord;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;

import java.util.List;

public class MemoryRecallAgentTest {
    public static void run() {
        EmbeddingModelGateway gateway = new EmbeddingModelGateway();
        MemoryEmbeddingJdbcRepository repository = new MemoryEmbeddingJdbcRepository();
        CoffeeRecord record = CoffeeRecord.archive(CurrentUserProvider.LOCAL_USER_ID, "s1", "d1", List.of("甜橙", "红茶", "水洗"));
        new MemoryEmbeddingConsumer(gateway, repository).handle(record);
        var recalls = new MemoryRecallService(gateway, repository).recall("s2", "水洗埃塞 甜橙 红茶", 3);
        ApiContractTestSupport.assertTrue(!recalls.isEmpty(), "相似记录必须可召回");
        ApiContractTestSupport.assertTrue(recalls.getFirst().possibleDuplicate(), "高相似记录必须提示可能重复");
        ApiContractTestSupport.assertContains(recalls.getFirst().matchedReasons().toString(), "甜橙", "召回必须说明相似原因");
    }
}
