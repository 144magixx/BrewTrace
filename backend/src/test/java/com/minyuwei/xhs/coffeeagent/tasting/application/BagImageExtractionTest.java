package com.minyuwei.xhs.coffeeagent.tasting.application;

import com.minyuwei.xhs.coffeeagent.shared.domain.ConfirmationStatus;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;

public class BagImageExtractionTest {
    public static void run() {
        var asset = new BagImageExtractionService().extract("s1", "/tmp/bag.jpg", "image/jpeg");
        ApiContractTestSupport.assertTrue(asset.confirmationStatus() == ConfirmationStatus.PENDING_CONFIRMATION, "豆袋解析结果默认必须待确认");
        ApiContractTestSupport.assertTrue(asset.extractedBeanFields().containsKey("name"), "豆袋解析应产出候选豆名字段");
    }
}
