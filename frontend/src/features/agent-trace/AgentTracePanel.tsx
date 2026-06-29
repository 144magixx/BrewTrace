import { renderAgentTraceCard, TraceCard } from "./AgentTraceCard";

export function renderAgentTracePanel(cards: TraceCard[]): string {
  return cards.map(renderAgentTraceCard).join("\n");
}

export function renderPublishingTraceCard(status: string, toolResult: string): string {
  return renderAgentTraceCard({
    stepType: "PUBLISH_CONFIRMATION",
    title: `发布状态:${status}`,
    summary: `工具结果:${toolResult}`,
    createdAt: new Date(0).toISOString()
  });
}
