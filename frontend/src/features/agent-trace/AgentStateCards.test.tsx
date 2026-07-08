import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AgentStateCards } from "./AgentStateCards";
import type { AgentStateSnapshot } from "../../services/workbenchTypes";

describe("AgentStateCards", () => {
  it("展示空状态", () => {
    render(<AgentStateCards />);

    expect(screen.getByText("当前没有可发送上下文。")).toBeInTheDocument();
  });

  it("展示来源、发送状态和高风险标签", () => {
    render(<AgentStateCards state={state()} />);

    expect(screen.getAllByText("来自用户消息").length).toBeGreaterThan(0);
    expect(screen.getAllByText("WILL_SEND").length).toBeGreaterThan(0);
    expect(screen.getByText("高风险")).toBeInTheDocument();
  });

  it("展示空候选记忆和模型边界", () => {
    render(<AgentStateCards state={state()} />);

    expect(screen.getByText("暂无候选记忆")).toBeInTheDocument();
    expect(screen.getByText("真实模型：已配置，等待返回")).toBeInTheDocument();
  });
});

function state(): AgentStateSnapshot {
  return {
    statusCards: [
      { id: "card1", type: "CONFIRMED_FACT", title: "已确认事实", summary: "用户确认风味：柑橘", sourceLabel: "来自用户消息", sendStatus: "WILL_SEND", riskLevel: "NONE", createdAt: "2026-06-30T00:00:00Z" },
      { id: "card2", type: "FACT_BOUNDARY_CHECK", title: "事实边界检查", summary: "检查结果：1 项", sourceLabel: "基于真实模型输出和当前事实", sendStatus: "PAGE_ONLY", riskLevel: "HIGH", createdAt: "2026-06-30T00:00:00Z" }
    ],
    contextItems: [{ id: "ctx1", role: "USER", content: "有柑橘和红茶感", sourceType: "USER_CONFIRMED", confirmationStatus: "CONFIRMED", sendStatus: "WILL_SEND", sourceMessageId: "m1", createdAt: "2026-06-30T00:00:00Z" }],
    confirmedFacts: [{ id: "fact1", factType: "FLAVOR", value: "用户确认风味：柑橘", sourceContextItemId: "ctx1", confirmationStatus: "CONFIRMED", sendStatus: "WILL_SEND" }],
    pendingAssociations: [],
    candidateMemories: [],
    contextPreview: {
      sections: [
        { sectionType: "CONFIRMED_FACTS", title: "已确认事实", items: [{ content: "用户确认风味：柑橘", sourceLabel: "来自用户消息", sendStatus: "WILL_SEND", exclusionReason: null }] },
        { sectionType: "CANDIDATE_MEMORIES", title: "候选记忆", items: [] }
      ],
      willSendCount: 1,
      excludedCount: 0,
      boundaryNote: "真实模型模式开启，以下内容已组织为 GPT-5.5 请求。"
    },
    modelOutput: null,
    factBoundaryChecks: [{ id: "check1", expression: "甜橙爆汁感很明显", basisType: "PENDING_ASSOCIATION", riskLevel: "HIGH", sourceReference: "由柑橘感扩展", message: "需要进一步确认。", recommendedAction: "ASK_USER_CONFIRMATION" }],
    capabilityBoundary: { realModelConnected: false, longTermMemoryConnected: false, xiaohongshuConnected: false, message: "已配置真实文本模型；等待输入或模型返回，未执行小红书动作，未接真实长期记忆数据库。" },
    sessionControlAction: { actionType: "CLEAR_CURRENT_SESSION", confirmationRequired: true, impactSummary: "清空当前会话可见状态。", confirmed: false, resultStatus: "IDLE" },
    updatedAt: "2026-06-30T00:00:00Z"
  };
}
