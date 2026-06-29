export type CurrentRecordPanelState = {
  beanFields: string[];
  brewFields: string[];
  flavorFields: string[];
  pendingConfirmations: string[];
};

export function CurrentRecordPanel(state: CurrentRecordPanelState): string {
  return [
    `咖啡豆:${state.beanFields.join(",")}`,
    `冲煮:${state.brewFields.join(",")}`,
    `风味:${state.flavorFields.join(",")}`,
    `待确认:${state.pendingConfirmations.join(",")}`
  ].join("\n");
}
