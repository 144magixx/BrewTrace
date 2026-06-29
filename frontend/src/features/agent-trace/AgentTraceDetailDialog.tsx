export function renderAgentTraceDetailDialog(snapshot: Record<string, unknown>): string {
  return JSON.stringify(snapshot, null, 2);
}
