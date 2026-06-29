package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.agent.application.ConversationWorkflowAgentTest;
import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftFactBoundaryTest;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionAgentTest;
import com.minyuwei.xhs.coffeeagent.tasting.api.TastingTemplateApiContractTest;
import com.minyuwei.xhs.coffeeagent.tasting.api.TastingSessionApiContractTest;
import com.minyuwei.xhs.coffeeagent.tasting.application.BagImageExtractionTest;

public final class TestRunner {
    private TestRunner() {
    }

    public static void main(String[] args) {
        FoundationSmokeTest.run();
        TastingSessionApiContractTest.run();
        ConversationWorkflowAgentTest.run();
        DraftFactBoundaryTest.run();
        TastingTemplateApiContractTest.run();
        FlavorSuggestionAgentTest.run();
        BagImageExtractionTest.run();
        System.out.println("backend behavior tests passed");
    }
}
