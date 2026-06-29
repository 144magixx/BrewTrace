export function renderExternalReferencePanel(items: string[]): string {
  return items.slice(0, 5).map(item => `外部参考:${item}:来源小红书`).join("\n");
}
