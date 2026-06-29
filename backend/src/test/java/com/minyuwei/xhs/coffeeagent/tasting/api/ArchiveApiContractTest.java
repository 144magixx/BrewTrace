package com.minyuwei.xhs.coffeeagent.tasting.api;

import com.minyuwei.xhs.coffeeagent.shared.application.DomainEventOutboxService;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tasting.application.ArchiveCoffeeRecordService;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;

import java.util.List;

public class ArchiveApiContractTest {
    public static void run() {
        DomainEventOutboxService outbox = new DomainEventOutboxService();
        ArchiveController controller = new ArchiveController(new ArchiveCoffeeRecordService(new CurrentUserProvider(), outbox));
        var response = controller.archive("req-archive", "s1", "d1", List.of("甜橙", "红茶"));
        ApiContractTestSupport.assertTrue(response.error() == null, "归档 API 必须成功返回 envelope");
        ApiContractTestSupport.assertTrue(response.data().record().flavorKeywords().contains("甜橙"), "归档记录必须保存风味关键词");
        ApiContractTestSupport.assertTrue(!response.data().createdPreferences().isEmpty(), "归档可生成偏好候选");
        ApiContractTestSupport.assertTrue(outbox.pendingRecords().size() == 1, "归档必须同步写入 Outbox");
    }
}
