import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ModelOutputPanel } from "./ModelOutputPanel";

describe("ModelOutputPanel", () => {
  it("展示真实模型输出标签、事实边界风险和建议动作", () => {
    render(<ModelOutputPanel
      output={{ outputType: "REAL_MODEL", messageType: "CONVERSATION", talk: "我还需要确认风味。", mode: "openai-gpt55", modelName: "gpt-5.5", statusLabel: "真实模型输出 / GPT-5.5", content: "甜橙爆汁感很明显。", sourceBoundary: "由真实模型生成，事实边界仍需检查。", post: null, conversation: { questions: ["这杯主要风味是什么？"], answerOptions: [{ id: "citrus", label: "柑橘感", content: "我喝到比较明显的柑橘感。" }], pendingConfirmations: [], warnings: [] }, warnings: [], variants: [], requestPreview: null, responsePreview: null, recoverableError: null, generatedAt: "2026-06-30T00:00:00Z" }}
      checks={[{ id: "check1", expression: "甜橙爆汁感很明显", basisType: "PENDING_ASSOCIATION", riskLevel: "WARNING", sourceReference: "由柑橘感扩展", message: "需要进一步确认。", recommendedAction: "ASK_USER_CONFIRMATION" }]}
    />);

    expect(screen.getByText("REAL_MODEL")).toBeInTheDocument();
    expect(screen.getByText("真实模型输出 / GPT-5.5")).toBeInTheDocument();
    expect(screen.getByText("PENDING_ASSOCIATION · ASK_USER_CONFIRMATION")).toBeInTheDocument();
    expect(screen.getByText("需确认")).toBeInTheDocument();
  });
});
