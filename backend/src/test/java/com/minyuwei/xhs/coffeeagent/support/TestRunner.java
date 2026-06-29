package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.agent.application.ConversationWorkflowAgentTest;
import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftFactBoundaryTest;
import com.minyuwei.xhs.coffeeagent.tasting.api.TastingSessionApiContractTest;

public final class TestRunner {
    private TestRunner() {
    }

    public static void main(String[] args) {
        FoundationSmokeTest.run();
        TastingSessionApiContractTest.run();
        ConversationWorkflowAgentTest.run();
        DraftFactBoundaryTest.run();
        System.out.println("backend behavior tests passed");
    }
}
