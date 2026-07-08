import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ConversationThread } from "./ConversationThread";
import type { DraftTab, ModelOutputSnapshot, WebConversationMessage } from "../../services/workbenchTypes";

describe("ConversationThread", () => {
  it("CONVERSATION 展示 talk 和追问问题，但不展示结构化字段名", () => {
    render(<ConversationThread messages={[userMessage("今天喝了一杯咖啡")]} drafts={[]} modelOutput={conversationOutput()} streamingAssistant={null} />);

    expect(screen.getByText(/我还需要确认风味和处理法/)).toBeInTheDocument();
    expect(screen.getByText(/这杯你喝到的主要风味是什么/)).toBeInTheDocument();
    expect(screen.queryByText("questions")).not.toBeInTheDocument();
    expect(screen.queryByText("主要风味仍需用户补充")).not.toBeInTheDocument();
  });

  it("POST 聊天气泡只展示 talk，不展示三版正文", () => {
    render(<ConversationThread messages={[userMessage("今天喝了一支水洗埃塞")]} drafts={draftTabs()} modelOutput={postOutput()} streamingAssistant={null} />);

    expect(screen.getByText("信息已经够了，我先整理成三版文案，你可以选一版再继续改。")).toBeInTheDocument();
    expect(screen.queryByText("GPT-5.5 克制版正文")).not.toBeInTheDocument();
    expect(screen.queryByText("GPT-5.5 夸张版正文")).not.toBeInTheDocument();
    expect(screen.queryByText("GPT-5.5 锐评版正文")).not.toBeInTheDocument();
  });

  it("等待首字时展示打字动画", () => {
    render(<ConversationThread messages={[userMessage("今天喝了一杯咖啡")]} drafts={[]} modelOutput={null} streamingAssistant={{ id: "a1", content: "", waitingForFirstToken: true }} />);

    expect(screen.getByLabelText("等待大模型首字回复")).toBeInTheDocument();
  });
});

function userMessage(content: string): WebConversationMessage {
  return { id: "m1", role: "USER", content, sourceType: "USER_CONFIRMED", createdAt: "2026-06-30T00:00:00Z" };
}

function conversationOutput(): ModelOutputSnapshot {
  return {
    outputType: "REAL_MODEL",
    messageType: "CONVERSATION",
    talk: "我还需要确认风味和处理法。",
    mode: "openai-gpt55",
    modelName: "gpt-5.5",
    statusLabel: "真实模型输出 / GPT-5.5",
    content: "",
    sourceBoundary: "由真实模型生成，事实边界仍需检查。",
    post: null,
    conversation: {
      questions: ["这杯你喝到的主要风味是什么？"],
      pendingConfirmations: [{ expression: "主要风味仍需用户补充", basisType: "PENDING_ASSOCIATION", sourceReference: "model.routing", sourceId: "", confidenceLabel: "LOW" }],
      warnings: []
    },
    warnings: [],
    variants: [],
    requestPreview: null,
    responsePreview: null,
    recoverableError: null,
    generatedAt: "2026-06-30T00:00:00Z"
  };
}

function postOutput(): ModelOutputSnapshot {
  return {
    ...conversationOutput(),
    messageType: "POST",
    talk: "信息已经够了，我先整理成三版文案，你可以选一版再继续改。",
    post: { variants: [], warnings: [] },
    conversation: null
  };
}

function draftTabs(): DraftTab[] {
  return [
    { draftId: "d1", style: "RESTRAINED", title: "克制版标题", body: "GPT-5.5 克制版正文", tags: ["咖啡"], factBoundaryNotes: [], reviewWarnings: [] },
    { draftId: "d2", style: "EXAGGERATED", title: "夸张版标题", body: "GPT-5.5 夸张版正文", tags: ["咖啡"], factBoundaryNotes: [], reviewWarnings: [] },
    { draftId: "d3", style: "SHARP_REVIEW", title: "锐评版标题", body: "GPT-5.5 锐评版正文", tags: ["咖啡"], factBoundaryNotes: [], reviewWarnings: [] }
  ];
}
