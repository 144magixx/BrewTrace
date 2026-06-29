package com.minyuwei.xhs.coffeeagent.publishing.domain;

import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;

import java.util.List;

public class PublishingPackageStateTest {
    public static void run() {
        PublishingPackage publishingPackage = new PublishingPackage("s1", "d1", "标题", "正文", List.of("咖啡"), List.of());
        boolean blocked = false;
        try {
            publishingPackage.publishAfterPreview(false);
        } catch (RuntimeException expected) {
            blocked = true;
        }
        ApiContractTestSupport.assertTrue(blocked, "未完成发布包确认和预览后二次确认时公开发布执行率必须为 0");
        publishingPackage.confirmPackage(true);
        publishingPackage.markXhsFilled();
        publishingPackage.publishAfterPreview(true);
        ApiContractTestSupport.assertTrue(publishingPackage.status() == PublishingPackage.Status.PUBLISHED, "二次确认后才能发布成功");
    }
}
