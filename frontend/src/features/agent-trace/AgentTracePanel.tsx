import { renderAgentTraceCard, TraceCard } from "./AgentTraceCard";

export function renderAgentTracePanel(cards: TraceCard[]): string {
  return cards.map(renderAgentTraceCard).join("\n");
}
