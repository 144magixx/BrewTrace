package com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.util.LinkedHashMap;
import java.util.Map;

public class FactBoundaryAdvisor implements CallAdvisor {
    private final int order;

    public FactBoundaryAdvisor() {
        this(50);
    }

    public FactBoundaryAdvisor(int order) {
        this.order = order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Map<String, Object> requestContext = new LinkedHashMap<>(request.context());
        requestContext.put(ModelAdvisorContextKeys.FACT_BOUNDARY_SUMMARY, factBoundarySummary(request.context()));
        ChatClientResponse response = chain.nextCall(request.mutate().context(requestContext).build());
        Map<String, Object> responseContext = new LinkedHashMap<>(requestContext);
        responseContext.putAll(response.context());
        return response.mutate().context(responseContext).build();
    }

    private Map<String, Object> factBoundarySummary(Map<String, Object> context) {
        Object value = context.get(ModelAdvisorContextKeys.MODEL_CONTEXT_PACKAGE);
        if (!(value instanceof ModelContextPackage contextPackage)) {
            return Map.of("status", "MISSING_CONTEXT_PACKAGE");
        }
        long willSendCurrentSession = contextPackage.currentSession().stream()
                .filter(entry -> "WILL_SEND".equals(entry.sendStatus()))
                .count();
        return Map.of(
                "status", "READY",
                "currentSessionWillSendCount", willSendCurrentSession,
                "confirmedFactCount", contextPackage.confirmedFacts().size(),
                "pendingAssociationCount", contextPackage.pendingAssociations().size(),
                "candidateMemoryCount", contextPackage.candidateMemoryBoundaries().size(),
                "excludedItemCount", contextPackage.excludedItems().size(),
                "constraintCount", contextPackage.promptConstraints().size()
        );
    }

    @Override
    public String getName() {
        return "coffee-agent-fact-boundary-advisor";
    }

    @Override
    public int getOrder() {
        return order;
    }
}
