export function renderSensoryRadar(scores: Record<string, number>): string {
  return Object.entries(scores).map(([dimension, value]) => `${dimension}=${value}`).join(",");
}
