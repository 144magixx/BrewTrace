package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.agent.application.ConversationWorkflowAgentTest;
import com.minyuwei.xhs.coffeeagent.copywriting.domain.DraftFactBoundaryTest;
import com.minyuwei.xhs.coffeeagent.flavor.application.FlavorSuggestionAgentTest;
import com.minyuwei.xhs.coffeeagent.memory.application.MemoryRecallAgentTest;
import com.minyuwei.xhs.coffeeagent.memory.infrastructure.MemoryEmbeddingJdbcTest;
import com.minyuwei.xhs.coffeeagent.shared.application.DomainEventOutboxTransactionTest;
import com.minyuwei.xhs.coffeeagent.tasting.api.ArchiveApiContractTest;
import com.minyuwei.xhs.coffeeagent.tasting.api.TastingTemplateApiContractTest;
import com.minyuwei.xhs.coffeeagent.tasting.api.TastingSessionApiContractTest;
import com.minyuwei.xhs.coffeeagent.tasting.application.BagImageExtractionTest;
import com.minyuwei.xhs.coffeeagent.trace.api.AgentTraceApiContractTest;
import com.minyuwei.xhs.coffeeagent.trace.api.AgentTraceSseTest;
import com.minyuwei.xhs.coffeeagent.trace.application.TraceSecretRedactionTest;

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
        ArchiveApiContractTest.run();
        MemoryEmbeddingJdbcTest.run();
        DomainEventOutboxTransactionTest.run();
        MemoryRecallAgentTest.run();
        AgentTraceApiContractTest.run();
        AgentTraceSseTest.run();
        TraceSecretRedactionTest.run();
        System.out.println("backend behavior tests passed");
    }
}
