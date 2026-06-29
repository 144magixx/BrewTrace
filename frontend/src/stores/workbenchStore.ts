export type OrchestrationMode = "EXPLICIT_WORKFLOW" | "MODEL_TOOL_CALLING";

export type WorkbenchState = {
  sessionId: string | null;
  mode: OrchestrationMode;
  traceCards: string[];
};

export function createInitialWorkbenchState(): WorkbenchState {
  return {
    sessionId: null,
    mode: "EXPLICIT_WORKFLOW",
    traceCards: []
  };
}

export function appendTraceCard(state: WorkbenchState, card: string): WorkbenchState {
  return {
    ...state,
    traceCards: [...state.traceCards, card]
  };
}
