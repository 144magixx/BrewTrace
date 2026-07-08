import { beforeEach, describe, expect, it } from "vitest";
import { clearSessionResume, readLocalResume, saveLocalResume } from "./localResumeStore";

describe("localResumeStore", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("清空 lastSessionId、draftInput 并记录 clearedSessionIds", () => {
    saveLocalResume({ lastSessionId: "s1", draftInput: "未提交草稿", lastKnownStatus: "DRAFTS_READY" });

    const next = clearSessionResume("s1");

    expect(next.lastSessionId).toBeNull();
    expect(next.draftInput).toBe("");
    expect(next.clearedSessionIds).toContain("s1");
    expect(readLocalResume().lastSessionId).toBeNull();
  });
});
