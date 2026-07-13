package com.minyuwei.xhs.coffeeagent.agent.infrastructure.advisor;

import com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地观测 Advisor，仅汇总调用前已保存的事实边界数量并写入 Trace 上下文。
 *
 * <p>该组件不理解用户语义、不执行关键词匹配，也不会额外调用模型；事实识别由主模型的
 * 结构化 {@code factUpdates} 一次性完成。</p>
 */
public class FactBoundaryAdvisor implements CallAdvisor {
    private final int order;

    /**
     * 使用默认顺序创建事实边界观测器。
     */
    public FactBoundaryAdvisor() {
        this(50);
    }

    /**
     * 使用指定 Advisor 顺序创建事实边界观测器。
     *
     * @param order 本地调用拦截器执行顺序
     */
    public FactBoundaryAdvisor(int order) {
        this.order = order;
    }

    @Override
    /**
     * 将事实边界计数写入调用上下文后继续同一次主模型调用。
     *
     * @param request 当前模型请求
     * @param chain 后续 Advisor 与模型调用链
     * @return 合并观测摘要后的模型响应
     */
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Map<String, Object> requestContext = new LinkedHashMap<>(request.context());
        requestContext.put(ModelAdvisorContextKeys.FACT_BOUNDARY_SUMMARY, factBoundarySummary(request.context()));
        ChatClientResponse response = chain.nextCall(request.mutate().context(requestContext).build());
        Map<String, Object> responseContext = new LinkedHashMap<>(requestContext);
        responseContext.putAll(response.context());
        return response.mutate().context(responseContext).build();
    }

    /**
     * 根据已组装状态快照计算不含用户原文的数量摘要。
     *
     * @param context Advisor 调用上下文
     * @return 可安全写入 Trace 的事实边界计数
     */
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
    /**
     * 返回 Spring AI Advisor 的稳定名称。
     *
     * @return Advisor 名称
     */
    public String getName() {
        return "coffee-agent-fact-boundary-advisor";
    }

    @Override
    /**
     * 返回该观测器在本地 Advisor 链中的顺序。
     *
     * @return 执行顺序
     */
    public int getOrder() {
        return order;
    }
}
