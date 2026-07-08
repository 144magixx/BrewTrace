import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import App from "./App";
import type { ApiResponse } from "../services/apiClient";
import type { AgentStateSnapshot, WorkbenchSnapshot } from "../services/workbenchTypes";

describe("App", () => {
  it("渲染真实工作台首屏且不是 landing page", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => jsonResponse(snapshot({ sessionId: null, status: "EMPTY" }))));

    render(<App />);

    expect(screen.getByRole("heading", { name: "今天喝了什么咖啡？" })).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: "开始记录" })).toBeInTheDocument();
  });

  it("固定展示 GPT-5.5 作为唯一模型模式", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => jsonResponse(snapshot({ sessionId: null, status: "EMPTY" }))));

    render(<App />);

    expect(await screen.findByText("本次提交：openai-gpt55")).toBeInTheDocument();
    expect(screen.getAllByText("上次运行：尚未运行").length).toBeGreaterThan(0);
    expect(screen.queryByRole("combobox", { name: "模型模式" })).not.toBeInTheDocument();
    expect(screen.queryByText("模型模式：openai-gpt55")).not.toBeInTheDocument();
  });

  it("提交咖啡记录后调用 GPT-5.5 并展示三版草稿", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(snapshot({ sessionId: null, status: "EMPTY" })))
      .mockResolvedValueOnce(jsonResponse(snapshot({ sessionId: "s1", status: "SESSION_CREATED" })))
      .mockResolvedValueOnce(jsonResponse(snapshot({
        sessionId: "s1",
        status: "DRAFTS_READY",
        conversation: [
          message("m1", "USER", "今天喝了一支水洗埃塞，有柑橘和红茶感")
        ],
        draftTabs: draftTabs(),
        recordSummary: {
          confirmedFacts: ["处理法：水洗", "产地：埃塞", "用户确认风味：柑橘", "用户确认风味：红茶"],
          pendingQuestions: [],
          suggestedFlavors: [],
          draftStatus: "VISIBLE",
          factBoundaryNotes: ["由真实模型生成，事实边界仍需检查。"]
        },
        agentState: agentStateWithModelOutput()
      })));
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);

    await user.click(await screen.findByRole("button", { name: "开始记录" }));
    await user.type(screen.getByLabelText("今天喝了什么咖啡？"), "今天喝了一支水洗埃塞，有柑橘和红茶感");
    await user.click(screen.getByRole("button", { name: "发送" }));

    expect(await screen.findAllByText("信息已经够了，我先整理成三版文案，你可以选一版再继续改。")).not.toHaveLength(0);
    expect(screen.getByRole("dialog", { name: "选择文案草稿" })).toBeInTheDocument();
    expect(screen.getAllByText("GPT-5.5 克制版标题").length).toBeGreaterThan(0);
    expect(screen.getAllByText("夸张版").length).toBeGreaterThan(0);
    expect(screen.getAllByText("锐评版").length).toBeGreaterThan(0);
    expect(screen.getAllByText("真实模型输出 / GPT-5.5").length).toBeGreaterThan(0);
    await user.click(screen.getByRole("button", { name: "选用此版" }));
    expect(screen.queryByRole("dialog", { name: "选择文案草稿" })).not.toBeInTheDocument();
    expect(screen.getByText("已选择此版作为后续修改基础")).toBeInTheDocument();
    expect(fetchMock).toHaveBeenLastCalledWith("/api/workbench/sessions/s1/messages/stream", expect.objectContaining({
      method: "POST",
      body: JSON.stringify({ content: "今天喝了一支水洗埃塞，有柑橘和红茶感", modelMode: "openai-gpt55" })
    }));
  });

  it("服务不可用时展示错误并保留输入", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn()
      .mockResolvedValueOnce(jsonResponse(snapshot({ sessionId: "s1", status: "SESSION_CREATED" })))
      .mockRejectedValueOnce(new Error("service unavailable")));

    render(<App />);

    const input = await screen.findByLabelText("今天喝了什么咖啡？");
    await user.type(input, "服务断开时也不要丢掉这段输入");
    await user.click(screen.getByRole("button", { name: "发送" }));

    expect(await screen.findByText("本地服务暂时不可用，已保留你的输入。")).toBeInTheDocument();
    expect(screen.getByDisplayValue("服务断开时也不要丢掉这段输入")).toBeInTheDocument();
  });

  it("模型返回备选回答时隐藏底部输入框并允许点击选项直接发送", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(snapshot({
        sessionId: "s1",
        status: "WAITING_FOR_FACTS",
        conversation: [message("m1", "ASSISTANT", "这杯你喝到最明显的风味是什么？")],
        agentState: agentStateWithQuestionAnswer()
      })))
      .mockResolvedValueOnce(jsonResponse(snapshot({
        sessionId: "s1",
        status: "WAITING_FOR_FACTS",
        conversation: [
          message("m1", "ASSISTANT", "这杯你喝到最明显的风味是什么？"),
          message("m2", "USER", "我喝到比较明显的柑橘感。")
        ],
        agentState: emptyAgentState()
      })));
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);

    expect(await screen.findByRole("dialog", { name: "这杯你喝到最明显的风味是什么？" })).toBeInTheDocument();
    expect(screen.queryByLabelText("今天喝了什么咖啡？")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /柑橘感/ }));

    await waitFor(() => expect(fetchMock).toHaveBeenLastCalledWith("/api/workbench/sessions/s1/messages/stream", expect.objectContaining({
      method: "POST",
      body: JSON.stringify({ content: "我喝到比较明显的柑橘感。", modelMode: "openai-gpt55" })
    })));
  });

  it("清空当前会话取消不改变状态，确认后清除恢复信息", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(snapshot({ sessionId: "s1", status: "DRAFTS_READY", conversation: [message("m1", "USER", "今天喝了一支水洗埃塞")], draftTabs: draftTabs(), agentState: agentStateWithModelOutput() })))
      .mockResolvedValueOnce(jsonResponse(snapshot({ sessionId: null, status: "EMPTY", agentState: emptyAgentState() })));
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);

    expect((await screen.findAllByText("GPT-5.5 克制版标题")).length).toBeGreaterThan(0);
    await user.click(screen.getByRole("button", { name: "新建记录 / 清空当前会话" }));
    await user.click(screen.getByRole("button", { name: "取消" }));
    expect(screen.getAllByText("GPT-5.5 克制版标题").length).toBeGreaterThan(0);
    expect(fetchMock).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole("button", { name: "新建记录 / 清空当前会话" }));
    await user.click(screen.getByRole("button", { name: "确认清空" }));

    await waitFor(() => expect(screen.queryAllByText("GPT-5.5 克制版标题")).toHaveLength(0));
    expect(screen.getAllByText(/当前没有可发送上下文/).length).toBeGreaterThan(0);
    expect(fetchMock).toHaveBeenLastCalledWith("/api/workbench/sessions/s1/clear", expect.objectContaining({ method: "POST" }));
  });
});

function jsonResponse<T>(data: T): Response {
  return new Response(JSON.stringify({ requestId: "test", data, error: null } satisfies ApiResponse<T>), {
    status: 200,
    headers: { "Content-Type": "application/json" }
  });
}

function snapshot(overrides: Partial<WorkbenchSnapshot>): WorkbenchSnapshot {
  return {
    sessionId: "s1",
    status: "SESSION_CREATED",
    heroQuestion: "今天喝了什么咖啡？",
    orchestrationMode: "EXPLICIT_WORKFLOW",
    conversation: [],
    recordSummary: {
      confirmedFacts: [],
      pendingQuestions: [],
      suggestedFlavors: [],
      draftStatus: "HIDDEN",
      factBoundaryNotes: []
    },
    draftTabs: [],
    agentState: emptyAgentState(),
    lastError: null,
    updatedAt: "2026-06-30T00:00:00Z",
    ...overrides
  };
}

function message(id: string, role: "USER" | "ASSISTANT", content: string) {
  return { id, role, content, sourceType: role === "USER" ? "USER_CONFIRMED" as const : "MODEL_SUGGESTED" as const, createdAt: "2026-06-30T00:00:00Z" };
}

function draftTabs() {
  const boundary = "模型联想仅作为待确认表达，不写成用户事实。";
  return [
    { draftId: "d1", style: "RESTRAINED" as const, title: "GPT-5.5 克制版标题", body: "GPT-5.5 克制版正文", tags: ["咖啡"], factBoundaryNotes: [boundary], reviewWarnings: [] },
    { draftId: "d2", style: "EXAGGERATED" as const, title: "GPT-5.5 夸张版标题", body: "GPT-5.5 夸张版正文", tags: ["咖啡"], factBoundaryNotes: [boundary], reviewWarnings: [] },
    { draftId: "d3", style: "SHARP_REVIEW" as const, title: "GPT-5.5 锐评版标题", body: "GPT-5.5 锐评版正文", tags: ["咖啡"], factBoundaryNotes: [boundary], reviewWarnings: [] }
  ];
}

function emptyAgentState(): AgentStateSnapshot {
  return {
    statusCards: [],
    contextItems: [],
    confirmedFacts: [],
    pendingAssociations: [],
    candidateMemories: [],
    contextPreview: { sections: [], willSendCount: 0, excludedCount: 0, boundaryNote: "当前没有可发送上下文。输入后将调用 GPT-5.5。" },
    modelOutput: null,
    factBoundaryChecks: [],
    capabilityBoundary: {
      realModelConnected: false,
      longTermMemoryConnected: false,
      xiaohongshuConnected: false,
      message: "已配置真实文本模型；等待输入或模型返回，未执行小红书动作，未接真实长期记忆数据库。"
    },
    sessionControlAction: {
      actionType: "CLEAR_CURRENT_SESSION",
      confirmationRequired: false,
      impactSummary: "清空当前会话可见状态和浏览器恢复状态，不删除长期记忆、历史归档或外部平台数据。",
      confirmed: false,
      resultStatus: "IDLE"
    },
    updatedAt: "2026-06-30T00:00:00Z"
  };
}

function agentStateAfterFirstTurn(): AgentStateSnapshot {
  return {
    ...emptyAgentState(),
    statusCards: [
      { id: "card-context", type: "SESSION_CONTEXT", title: "当前会话上下文", summary: "当前会话项：2", sourceLabel: "来自当前页面会话", sendStatus: "WILL_SEND", riskLevel: "INFO", createdAt: "2026-06-30T00:00:00Z" },
      { id: "card-memory", type: "CANDIDATE_MEMORY", title: "候选记忆", summary: "候选记忆：1", sourceLabel: "未接真实长期数据库", sendStatus: "PAGE_ONLY", riskLevel: "INFO", createdAt: "2026-06-30T00:00:00Z" }
    ],
    contextItems: [{ id: "c1", role: "USER", content: "今天喝了一支水洗埃塞，有柑橘和红茶感", sourceType: "USER_CONFIRMED", confirmationStatus: "CONFIRMED", sendStatus: "WILL_SEND", sourceMessageId: "m1", createdAt: "2026-06-30T00:00:00Z" }],
    confirmedFacts: [{ id: "f1", factType: "FLAVOR", value: "用户确认风味：柑橘", sourceContextItemId: "c1", confirmationStatus: "CONFIRMED", sendStatus: "WILL_SEND" }],
    pendingAssociations: [],
    candidateMemories: [],
    contextPreview: {
      sections: [
        { sectionType: "CONFIRMED_FACTS", title: "已确认事实", items: [{ content: "用户确认风味：柑橘", sourceLabel: "来自用户消息", sendStatus: "WILL_SEND", exclusionReason: null }] },
        { sectionType: "PENDING_ASSOCIATIONS", title: "待确认联想", items: [] },
        { sectionType: "CANDIDATE_MEMORIES", title: "候选记忆", items: [] }
      ],
      willSendCount: 1,
      excludedCount: 0,
      boundaryNote: "真实模型模式开启，以下内容已组织为 GPT-5.5 请求。"
    }
  };
}

function agentStateWithModelOutput(): AgentStateSnapshot {
  return {
    ...agentStateAfterFirstTurn(),
    modelOutput: {
      outputType: "REAL_MODEL",
      messageType: "POST",
      talk: "信息已经够了，我先整理成三版文案，你可以选一版再继续改。",
      mode: "openai-gpt55",
      modelName: "gpt-5.5",
      statusLabel: "真实模型输出 / GPT-5.5",
      content: "真实模型生成正文。",
      sourceBoundary: "由真实模型生成，事实边界仍需检查。",
      post: {
        variants: [],
        warnings: []
      },
      conversation: null,
      warnings: [],
      variants: [],
      requestPreview: null,
      responsePreview: null,
      recoverableError: null,
      generatedAt: "2026-06-30T00:00:00Z"
    },
    factBoundaryChecks: [
      { id: "check1", expression: "甜橙爆汁感很明显", basisType: "PENDING_ASSOCIATION", riskLevel: "WARNING", sourceReference: "由柑橘感扩展", message: "用户只确认了柑橘感，甜橙爆汁感需要进一步确认。", recommendedAction: "ASK_USER_CONFIRMATION" }
    ]
  };
}

function agentStateWithQuestionAnswer(): AgentStateSnapshot {
  return {
    ...agentStateAfterFirstTurn(),
    modelOutput: {
      outputType: "REAL_MODEL",
      messageType: "CONVERSATION",
      talk: "听起来不错，你这杯喝到最明显的风味是什么？",
      mode: "openai-gpt55",
      modelName: "gpt-5.5",
      statusLabel: "真实模型输出 / GPT-5.5",
      content: "",
      sourceBoundary: "由真实模型生成，事实边界仍需检查。",
      post: null,
      conversation: {
        questions: ["这杯你喝到最明显的风味是什么？"],
        answerOptions: [
          { id: "citrus", label: "柑橘感", content: "我喝到比较明显的柑橘感。" },
          { id: "black-tea", label: "红茶感", content: "我喝到一点红茶感。" },
          { id: "not-sure", label: "说不清", content: "我暂时说不太清楚，只觉得整体比较干净。" }
        ],
        pendingConfirmations: [],
        warnings: []
      },
      warnings: [],
      variants: [],
      requestPreview: null,
      responsePreview: null,
      recoverableError: null,
      generatedAt: "2026-06-30T00:00:00Z"
    }
  };
}
