package com.minyuwei.xhs.coffeeagent.tasting.api;

import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.infrastructure.TastingSessionRepositoryAdapter;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;

public class TastingSessionApiContractTest {
    public static void run() {
        TastingSessionController controller = new TastingSessionController(new TastingSessionApplicationService(
                new TastingSessionRepositoryAdapter(),
                new CurrentUserProvider()
        ));
        var created = controller.createSession("req-us1", OrchestrationMode.EXPLICIT_WORKFLOW);
        ApiContractTestSupport.assertTrue("req-us1".equals(created.requestId()), "创建会话响应必须保留 requestId");
        String sessionId = created.data().get("sessionId");
        var response = controller.submitMessage("req-us1-msg", sessionId, "今天喝了一支水洗埃塞，有柑橘和红茶感");
        ApiContractTestSupport.assertTrue(response.error() == null, "提交消息必须使用成功 envelope");
        ApiContractTestSupport.assertTrue(response.data().pendingQuestions().isEmpty(), "提交消息不应触发本地追问链路");
        ApiContractTestSupport.assertContains(response.data().assistantMessage(), "GPT-5.5", "提交消息必须交给 GPT-5.5 工作台链路");
    }
}
