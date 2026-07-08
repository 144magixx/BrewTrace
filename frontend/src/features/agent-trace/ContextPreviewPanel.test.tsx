import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ContextPreviewPanel } from "./ContextPreviewPanel";

describe("ContextPreviewPanel", () => {
  it("展示发送、排除、待确认和仅页面观察四类状态", () => {
    render(<ContextPreviewPanel preview={{
      willSendCount: 1,
      excludedCount: 1,
      boundaryNote: "真实模型模式开启，以下内容已组织为 GPT-5.5 请求。",
      sections: [
        { sectionType: "CURRENT_SESSION", title: "当前会话", items: [{ content: "用户确认风味：柑橘", sourceLabel: "来自用户消息", sendStatus: "WILL_SEND", exclusionReason: null }] },
        { sectionType: "PENDING_ASSOCIATIONS", title: "待确认联想", items: [{ content: "甜橙", sourceLabel: "由柑橘感扩展", sendStatus: "SEND_AFTER_CONFIRMATION", exclusionReason: "用户确认后才可能发送" }] },
        { sectionType: "CANDIDATE_MEMORIES", title: "候选记忆", items: [
          { content: "长期记忆候选", sourceLabel: "真实长期数据库召回", sendStatus: "PAGE_ONLY", exclusionReason: "仅页面观察，不会发送给模型" },
          { content: "冲突示例", sourceLabel: "不是真实长期数据库召回", sendStatus: "EXCLUDED", exclusionReason: "存在冲突或无依据，已排除" }
        ] }
      ]
    }} />);

    expect(screen.getByText("WILL_SEND")).toBeInTheDocument();
    expect(screen.getByText("SEND_AFTER_CONFIRMATION")).toBeInTheDocument();
    expect(screen.getByText("PAGE_ONLY")).toBeInTheDocument();
    expect(screen.getByText("EXCLUDED")).toBeInTheDocument();
  });
});
