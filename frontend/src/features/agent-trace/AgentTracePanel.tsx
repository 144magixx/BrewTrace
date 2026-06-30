import { renderAgentTraceCard, TraceCard } from "./AgentTraceCard";

export function AgentTracePanel({ steps }: { steps: string[] }) {
  return (
    <section className="trace-panel">
      <h2>Agent 状态</h2>
      <ol>
        {steps.map((step) => <li key={step}>{step}</li>)}
      </ol>
      <p>完整 AgentTrace SSE 会在后续切片接入。</p>
    </section>
  );
}

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
