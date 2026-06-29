package com.minyuwei.xhs.coffeeagent.publishing.api;

import com.minyuwei.xhs.coffeeagent.publishing.application.ExternalReferenceService;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;

public class ExternalReferenceApiContractTest {
    public static void run() {
        var response = new ExternalReferenceController(new ExternalReferenceService()).search("req-ref", "s1", "水洗埃塞 柑橘 红茶", 9);
        ApiContractTestSupport.assertTrue(response.data().size() == 5, "外部参考最多返回 5 条");
        ApiContractTestSupport.assertContains(response.data().getFirst().summary(), "不是用户事实", "外部参考必须标明事实边界");
    }
}
