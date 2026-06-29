export type MemoryRecallCard = {
  summary: string;
  matchedReasons: string[];
  possibleDuplicate: boolean;
};

export function renderMemoryRecallPanel(cards: MemoryRecallCard[]): string {
  return cards.map(card => `${card.summary}:${card.matchedReasons.join(",")}:${card.possibleDuplicate ? "可能重复" : "相似参考"}`).join("\n");
}
