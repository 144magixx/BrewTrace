package com.minyuwei.xhs.coffeeagent.copywriting.domain;

import com.minyuwei.xhs.coffeeagent.agent.application.ContextAssembler;
import com.minyuwei.xhs.coffeeagent.agent.application.DraftAgent;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;

import java.util.List;

public class DraftFactBoundaryTest {
    public static void run() {
        var context = new ContextAssembler.AgentContext(
                "s1",
                List.of("用户确认风味：柑橘", "用户确认风味：红茶"),
                List.of("甜橙", "青柠", "葡萄柚"),
                OrchestrationMode.EXPLICIT_WORKFLOW
        );
        List<DraftCopy> drafts = new DraftAgent().generate(context);
        for (DraftCopy draft : drafts) {
            ApiContractTestSupport.assertTrue(!draft.writesUnconfirmedFlavorAsFact(context.unconfirmedFlavorCandidates()), "未确认风味不得写成事实：" + draft.body());
            ApiContractTestSupport.assertContains(String.join(" ", draft.factBoundaryNotes()), "待确认联想", "文案必须附带事实边界说明");
        }
    }
}
