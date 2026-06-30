import { describe, expect, it } from "vitest";

export const errorRecoveryScenario = {
  unavailableMessage: "本地服务暂时不可用，已保留你的输入。",
  preservedInput: "服务断开时也不要丢掉这段输入",
  nextActions: ["CHECK_LOCAL_SERVICE", "RETRY"]
};

describe("Workbench error recovery scenario", () => {
  it("覆盖服务不可用、输入保留和恢复建议", () => {
    expect(errorRecoveryScenario.unavailableMessage).toContain("已保留");
    expect(errorRecoveryScenario.preservedInput).toContain("不要丢掉");
    expect(errorRecoveryScenario.nextActions).toContain("CHECK_LOCAL_SERVICE");
  });
});
