import { renderMemoryRecallPanel } from "./MemoryRecallPanel";
import { renderUserPreferenceEditor } from "./UserPreferenceEditor";

export function memoryPanelFixture(): string {
  const recall = renderMemoryRecallPanel([
    { summary: "甜橙、红茶历史记录", matchedReasons: ["相同风味关键词：甜橙", "相同风味关键词：红茶"], possibleDuplicate: true }
  ]);
  const preference = renderUserPreferenceEditor([
    { value: "甜橙", evidence: "来自归档记录", status: "CANDIDATE" }
  ]);
  return `${recall}\n${preference}`;
}
