package com.minyuwei.xhs.coffeeagent.support;

import com.minyuwei.xhs.coffeeagent.memory.application.MemoryRecallAgentTest;
import com.minyuwei.xhs.coffeeagent.memory.infrastructure.MemoryEmbeddingJdbcTest;
import com.minyuwei.xhs.coffeeagent.publishing.api.ExternalReferenceApiContractTest;
import com.minyuwei.xhs.coffeeagent.publishing.domain.PublishingPackageStateTest;
import com.minyuwei.xhs.coffeeagent.shared.application.DomainEventOutboxTransactionTest;
import com.minyuwei.xhs.coffeeagent.tasting.api.ArchiveApiContractTest;
import com.minyuwei.xhs.coffeeagent.tasting.api.TastingTemplateApiContractTest;
import com.minyuwei.xhs.coffeeagent.tasting.api.TastingSessionApiContractTest;
import com.minyuwei.xhs.coffeeagent.tasting.application.BagImageExtractionTest;
import com.minyuwei.xhs.coffeeagent.trace.api.AgentTraceApiContractTest;
import com.minyuwei.xhs.coffeeagent.trace.api.AgentTraceSseTest;
import com.minyuwei.xhs.coffeeagent.trace.application.TraceSecretRedactionTest;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.ImageGenerationToolAdapterTest;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.FlavorSuggestionToolAdapterTest;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.SpringAiToolCallbackAdapterTest;
import com.minyuwei.xhs.coffeeagent.tools.infrastructure.XiaohongshuToolAdapterTest;

public final class TestRunner {
    private TestRunner() {
    }

    public static void main(String[] args) {
        FoundationSmokeTest.run();
        TastingSessionApiContractTest.run();
        TastingTemplateApiContractTest.run();
        BagImageExtractionTest.run();
        ArchiveApiContractTest.run();
        MemoryEmbeddingJdbcTest.run();
        DomainEventOutboxTransactionTest.run();
        MemoryRecallAgentTest.run();
        AgentTraceApiContractTest.run();
        AgentTraceSseTest.run();
        TraceSecretRedactionTest.run();
        ExternalReferenceApiContractTest.run();
        PublishingPackageStateTest.run();
        FlavorSuggestionToolAdapterTest.run();
        SpringAiToolCallbackAdapterTest.run();
        XiaohongshuToolAdapterTest.run();
        ImageGenerationToolAdapterTest.run();
        ModelMessageRoutingBehaviorTest.run();
        System.out.println("backend behavior tests passed");
    }
}
