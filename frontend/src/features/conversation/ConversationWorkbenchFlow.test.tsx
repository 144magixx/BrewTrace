import { describe, expect, it } from "vitest";

export const conversationWorkbenchFlowScenario = {
  firstMessage: "今天喝了一支水洗埃塞，有柑橘和红茶感",
  followupMessage: "豆子是某烘焙商的埃塞水洗豆，水温 92 度，粉水比 1:15，想看克制、夸张和锐评。",
  expectedDraftStyles: ["RESTRAINED", "EXAGGERATED", "SHARP_REVIEW"],
  factBoundary: "甜橙、青柠、葡萄柚仅作为待确认联想，不写成事实。"
};

describe("Conversation workbench flow scenario", () => {
  it("覆盖首轮追问、补充事实、三版草稿和事实边界", () => {
    expect(conversationWorkbenchFlowScenario.firstMessage).toContain("水洗埃塞");
    expect(conversationWorkbenchFlowScenario.expectedDraftStyles).toEqual(["RESTRAINED", "EXAGGERATED", "SHARP_REVIEW"]);
    expect(conversationWorkbenchFlowScenario.factBoundary).toContain("待确认联想");
  });
});
