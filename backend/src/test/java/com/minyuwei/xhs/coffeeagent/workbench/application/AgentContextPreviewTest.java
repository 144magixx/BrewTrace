package com.minyuwei.xhs.coffeeagent.workbench.application;

import com.minyuwei.xhs.coffeeagent.agent.application.FactUpdate;
import com.minyuwei.xhs.coffeeagent.agent.application.OrchestrationMode;
import com.minyuwei.xhs.coffeeagent.tasting.application.TastingSessionApplicationService;
import com.minyuwei.xhs.coffeeagent.tasting.domain.ConversationMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentContextPreviewTest {
    /**
     * 验证工作台预览按边界展示当前会话和已确认事实。
     */
    @Test
    void previewGroupsCurrentSessionAndConfirmedFactsForGpt55Request() {
        AgentStateAssembler assembler = new AgentStateAssembler(new FactBoundaryChecker());
        ConversationMessage source = ConversationMessage.user("s1", "今天喝了一支水洗埃塞，有柑橘和红茶感");
        Instant now = Instant.now();
        var state = assembler.assemble(new TastingSessionApplicationService.WorkspaceSnapshot(
                "s1",
                "今天喝了什么咖啡？",
                OrchestrationMode.EXPLICIT_WORKFLOW,
                List.of(source),
                List.of(),
                List.of(
                        fact("fact-1", "处理法：水洗", source, "水洗", now),
                        fact("fact-2", "产地：埃塞", source, "埃塞", now),
                        fact("fact-3", "用户确认风味：柑橘", source, "柑橘", now),
                        fact("fact-4", "用户确认风味：红茶", source, "红茶", now)
                ),
                List.of(), List.of(), List.of()
        ));

        assertTrue(state.contextPreview().sections().stream().anyMatch(section -> section.sectionType().equals("CURRENT_SESSION")));
        assertTrue(state.contextPreview().sections().stream().anyMatch(section -> section.sectionType().equals("CONFIRMED_FACTS")));
        assertEquals(5, state.contextPreview().willSendCount());
        assertTrue(state.pendingAssociations().isEmpty());
        assertTrue(state.candidateMemories().isEmpty());
    }

    /**
     * 验证助手追问进入下一轮模型上下文，但不会被映射成已确认事实。
     */
    @Test
    void sendsAssistantQuestionAsConversationContextOnly() {
        CapturingGateway gateway = new CapturingGateway();
        AgentStateAssembler assembler = new AgentStateAssembler(new FactBoundaryChecker(), gateway);
        List<ConversationMessage> conversation = List.of(
                ConversationMessage.user("s1", "有柑橘风味"),
                ConversationMessage.assistant("s1", "这个柑橘更像甜橙还是柠檬？"),
                ConversationMessage.user("s1", "更像甜橙。")
        );
        TastingSessionApplicationService.WorkspaceSnapshot workspace = new TastingSessionApplicationService.WorkspaceSnapshot(
                "s1", "今天喝了什么咖啡？", OrchestrationMode.EXPLICIT_WORKFLOW,
                conversation, List.of(), List.of(), List.of(), List.of(), List.of());

        assembler.completeModel(workspace, null);

        assertEquals(List.of("USER", "ASSISTANT", "USER"),
                gateway.context.currentSession().stream().map(com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage.ContextEntry::role).toList());
        assertEquals(0, gateway.context.confirmedFacts().size());
    }

    private com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem fact(
            String id, String value, ConversationMessage source, String quote, Instant now) {
        return new com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem(
                id, com.minyuwei.xhs.coffeeagent.tasting.domain.FactStateItem.Status.CONFIRMED,
                FactUpdate.Boundary.USER_STATED, value, source.id(), quote, "用户明确陈述", now, now);
    }

    private static final class CapturingGateway implements com.minyuwei.xhs.coffeeagent.agent.application.ModelGateway {
        private com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage context;

        /**
         * 保存待发送上下文，供测试校验会话角色和事实边界。
         *
         * @param contextPackage 本轮模型调用上下文
         * @return 最小成功结果，测试不依赖真实模型
         */
        @Override
        public ModelResult complete(com.minyuwei.xhs.coffeeagent.agent.application.ModelContextPackage contextPackage) {
            this.context = contextPackage;
            return null;
        }
    }
}
