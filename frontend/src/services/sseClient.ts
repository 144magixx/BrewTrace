export type WorkbenchEvent = {
  event: string;
  id?: string;
  data: Record<string, unknown>;
};

export function parseSseLine(event: string, data: string): WorkbenchEvent {
  return { event, data: JSON.parse(data) };
}
