import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import App from "./App";
import type { ApiResponse } from "../services/apiClient";
import type { WorkbenchSnapshot } from "../services/workbenchTypes";

describe("App", () => {
  it("渲染真实工作台首屏且不是 landing page", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => jsonResponse(snapshot({ sessionId: null, status: "EMPTY" }))));

    render(<App />);

    expect(screen.getByRole("heading", { name: "今天喝了什么咖啡？" })).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: "开始记录" })).toBeInTheDocument();
  });

  it("完成首轮追问和补充事实生成三版草稿", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(jsonResponse(snapshot({ sessionId: null, status: "EMPTY" })))
      .mockResolvedValueOnce(jsonResponse(snapshot({ sessionId: "s1", status: "SESSION_CREATED" })))
      .mockResolvedValueOnce(jsonResponse(snapshot({
        sessionId: "s1",
        status: "WAITING_FOR_FACTS",
        conversation: [
          message("m1", "USER", "今天喝了一支水洗埃塞，有柑橘和红茶感"),
          message("m2", "ASSISTANT", "还需要确认豆子信息、冲煮参数和你想要的文案风格。")
        ],
        recordSummary: {
          confirmedFacts: ["处理法：水洗", "产地：埃塞", "用户确认风味：柑橘", "用户确认风味：红茶"],
          pendingQuestions: ["豆名或烘焙商是什么？"],
          suggestedFlavors: ["甜橙", "青柠", "葡萄柚"],
          draftStatus: "HIDDEN",
          factBoundaryNotes: []
        }
      })))
      .mockResolvedValueOnce(jsonResponse(snapshot({
        sessionId: "s1",
        status: "DRAFTS_READY",
        conversation: [
          message("m1", "USER", "今天喝了一支水洗埃塞，有柑橘和红茶感"),
          message("m2", "ASSISTANT", "还需要确认豆子信息、冲煮参数和你想要的文案风格。"),
          message("m3", "USER", "豆子是某烘焙商的埃塞水洗豆，水温 92 度，粉水比 1:15，想看克制、夸张和锐评。"),
          message("m4", "ASSISTANT", "信息已经足够，我会基于已确认事实生成三版文案。")
        ],
        draftTabs: draftTabs(),
        recordSummary: {
          confirmedFacts: ["处理法：水洗", "产地：埃塞", "用户确认风味：柑橘", "用户确认风味：红茶", "已补充冲煮参数"],
          pendingQuestions: [],
          suggestedFlavors: ["甜橙", "青柠", "葡萄柚"],
          draftStatus: "VISIBLE",
          factBoundaryNotes: ["甜橙、青柠、葡萄柚仅作为待确认联想，不写成事实。"]
        }
      })));
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);

    await user.click(await screen.findByRole("button", { name: "开始记录" }));
    await user.type(screen.getByLabelText("今天喝了什么咖啡？"), "今天喝了一支水洗埃塞，有柑橘和红茶感");
    await user.click(screen.getByRole("button", { name: "发送" }));
    expect(await screen.findByText("还需要确认豆子信息、冲煮参数和你想要的文案风格。")).toBeInTheDocument();
    expect(screen.queryByText("一杯干净的水洗埃塞")).not.toBeInTheDocument();

    await user.type(screen.getByLabelText("今天喝了什么咖啡？"), "豆子是某烘焙商的埃塞水洗豆，水温 92 度，粉水比 1:15，想看克制、夸张和锐评。");
    await user.click(screen.getByRole("button", { name: "发送" }));

    expect(await screen.findByText("一杯干净的水洗埃塞")).toBeInTheDocument();
    expect(screen.getByText("夸张版")).toBeInTheDocument();
    expect(screen.getByText("锐评版")).toBeInTheDocument();
    expect(screen.getAllByText("甜橙、青柠、葡萄柚仅作为待确认联想，不写成事实。").length).toBeGreaterThan(0);
  });

  it("服务不可用时展示错误并保留输入", async () => {
    const user = userEvent.setup();
    vi.stubGlobal("fetch", vi.fn()
      .mockResolvedValueOnce(jsonResponse(snapshot({ sessionId: "s1", status: "SESSION_CREATED" })))
      .mockRejectedValueOnce(new Error("offline")));

    render(<App />);

    const input = await screen.findByLabelText("今天喝了什么咖啡？");
    await user.type(input, "服务断开时也不要丢掉这段输入");
    await user.click(screen.getByRole("button", { name: "发送" }));

    expect(await screen.findByText("本地服务暂时不可用，已保留你的输入。")).toBeInTheDocument();
    expect(screen.getByDisplayValue("服务断开时也不要丢掉这段输入")).toBeInTheDocument();
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
    lastError: null,
    updatedAt: "2026-06-30T00:00:00Z",
    ...overrides
  };
}

function message(id: string, role: "USER" | "ASSISTANT", content: string) {
  return { id, role, content, sourceType: role === "USER" ? "USER_CONFIRMED" as const : "MODEL_SUGGESTED" as const, createdAt: "2026-06-30T00:00:00Z" };
}

function draftTabs() {
  const boundary = "甜橙、青柠、葡萄柚仅作为待确认联想，不写成事实。";
  return [
    { draftId: "d1", style: "RESTRAINED" as const, title: "一杯干净的水洗埃塞", body: "克制版正文", tags: ["咖啡"], factBoundaryNotes: [boundary], reviewWarnings: [] },
    { draftId: "d2", style: "EXAGGERATED" as const, title: "柑橘光线落进红茶里", body: "夸张版正文", tags: ["咖啡"], factBoundaryNotes: [boundary], reviewWarnings: [] },
    { draftId: "d3", style: "SHARP_REVIEW" as const, title: "好喝，但别急着神化", body: "锐评版正文", tags: ["咖啡"], factBoundaryNotes: [boundary], reviewWarnings: [] }
  ];
}
