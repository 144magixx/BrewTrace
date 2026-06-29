import { renderAgentTracePanel } from "./AgentTracePanel";
import { renderAgentTraceDetailDialog } from "./AgentTraceDetailDialog";

export function agentTracePanelFixture(): string {
  const panel = renderAgentTracePanel([
    { stepType: "USER_INPUT", title: "用户输入", summary: "水洗埃塞", createdAt: "2026-06-30 03:00:00" },
    { stepType: "CONTEXT_BUILD", title: "上下文组装", summary: "已组装事实", createdAt: "2026-06-30 03:00:01" },
    { stepType: "MODEL_CALL", title: "模型调用", summary: "生成文案", createdAt: "2026-06-30 03:00:02" },
    { stepType: "MEMORY_RECALL", title: "记忆召回", summary: "召回相似记录", createdAt: "2026-06-30 03:00:03" },
    { stepType: "REVIEW", title: "审稿", summary: "事实边界通过", createdAt: "2026-06-30 03:00:04" }
  ]);
  const detail = renderAgentTraceDetailDialog({ promptSnapshot: { messages: [] }, toolOutputSnapshot: { status: "ok" } });
  return `${panel}\n${detail}`;
}
