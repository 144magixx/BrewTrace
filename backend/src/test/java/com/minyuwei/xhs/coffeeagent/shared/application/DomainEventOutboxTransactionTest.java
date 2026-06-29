package com.minyuwei.xhs.coffeeagent.shared.application;

import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tasting.application.ArchiveCoffeeRecordService;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;

import java.util.List;

public class DomainEventOutboxTransactionTest {
    public static void run() {
        DomainEventOutboxService outbox = new DomainEventOutboxService();
        ArchiveCoffeeRecordService archiveService = new ArchiveCoffeeRecordService(new CurrentUserProvider(), outbox);
        archiveService.archive("s1", "d1", List.of("甜橙", "红茶"), true);
        ApiContractTestSupport.assertTrue(archiveService.records().size() == 1, "核心记录必须保存");
        ApiContractTestSupport.assertTrue(outbox.pendingRecords().size() == 1, "Outbox 必须与核心记录同命令写入");
        new com.minyuwei.xhs.coffeeagent.shared.infrastructure.kafka.OutboxPublisher(outbox).publishPending();
        ApiContractTestSupport.assertTrue(outbox.pendingRecords().isEmpty(), "发布后不应残留 PENDING 事件");
    }
}
