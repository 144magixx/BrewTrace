export type TraceCard = {
  stepType: "USER_INPUT" | "CONTEXT_BUILD" | "MODEL_CALL" | "TOOL_CALL" | "MEMORY_RECALL" | "REVIEW" | "PUBLISH_CONFIRMATION" | "SYSTEM_STATUS";
  title: string;
  summary: string;
  createdAt: string;
};

export function renderAgentTraceCard(card: TraceCard): string {
  return `${card.stepType}:${card.title}:${card.summary}:${card.createdAt}`;
}
