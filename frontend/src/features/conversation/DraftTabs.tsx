export type DraftTab = {
  style: "RESTRAINED" | "EXAGGERATED" | "SHARP_REVIEW";
  title: string;
  body: string;
};

export function renderDraftTabs(tabs: DraftTab[]): string {
  return tabs.map(tab => `${tab.style}:${tab.title}`).join("|");
}
