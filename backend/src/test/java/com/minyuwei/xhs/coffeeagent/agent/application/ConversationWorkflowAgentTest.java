package com.minyuwei.xhs.coffeeagent.agent.application;

import com.minyuwei.xhs.coffeeagent.support.ApiContractTestSupport;
import com.minyuwei.xhs.coffeeagent.tasting.domain.TastingSession;
import com.minyuwei.xhs.coffeeagent.user.application.CurrentUserProvider;

public class ConversationWorkflowAgentTest {
    public static void run() {
        TastingSession session = TastingSession.create(CurrentUserProvider.LOCAL_USER_ID, OrchestrationMode.EXPLICIT_WORKFLOW);
        session.addUserMessage("今天喝了一支水洗埃塞，有柑橘和红茶感");
        AgentOrchestrator.TurnResult firstTurn = new AgentOrchestrator().handleUserTurn(session);
        ApiContractTestSupport.assertTrue(firstTurn.pendingQuestions().size() >= 2, "首轮信息不足必须追问豆子和冲煮参数");

        session.addUserMessage("豆子是某烘焙商的埃塞水洗豆，水温92度，粉水比1:15，想看克制、夸张和锐评。");
        AgentOrchestrator.TurnResult secondTurn = new AgentOrchestrator().handleUserTurn(session);
        ApiContractTestSupport.assertTrue(secondTurn.pendingQuestions().isEmpty(), "补充事实后不应继续阻塞生成");
        ApiContractTestSupport.assertTrue(secondTurn.drafts().size() == 3, "必须生成三类文案草稿");
        ApiContractTestSupport.assertTrue(secondTurn.plannedSteps().contains("REVIEW_FACT_BOUNDARY"), "显式工作流必须包含事实边界审稿步骤");
    }
}
