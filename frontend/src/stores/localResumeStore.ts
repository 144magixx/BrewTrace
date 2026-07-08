import type { WorkbenchStatus } from "../services/workbenchTypes";

const STORAGE_KEY = "coffee-agent.local-resume";

export type LocalResumeState = {
  lastSessionId: string | null;
  draftInput: string;
  lastKnownStatus: WorkbenchStatus | "UNKNOWN";
  clearedSessionIds: string[];
  savedAt: string;
};

export function readLocalResume(): LocalResumeState {
  const fallback: LocalResumeState = { lastSessionId: null, draftInput: "", lastKnownStatus: "UNKNOWN", clearedSessionIds: [], savedAt: new Date(0).toISOString() };
  if (typeof localStorage === "undefined") {
    return fallback;
  }
  let raw: string | null;
  try {
    raw = localStorage.getItem(STORAGE_KEY);
  } catch {
    return fallback;
  }
  if (!raw) {
    return fallback;
  }
  try {
    const parsed = { ...fallback, ...JSON.parse(raw) } as LocalResumeState;
    return parsed.lastSessionId && parsed.clearedSessionIds.includes(parsed.lastSessionId)
      ? { ...parsed, lastSessionId: null, draftInput: "" }
      : parsed;
  } catch {
    return fallback;
  }
}

export function saveLocalResume(state: Partial<LocalResumeState>): LocalResumeState {
  const next: LocalResumeState = {
    ...readLocalResume(),
    ...state,
    savedAt: new Date().toISOString()
  };
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    return next;
  }
  return next;
}

export function clearLocalResume(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Local resume is best-effort and must not block the workbench.
  }
}

export function clearSessionResume(sessionId: string): LocalResumeState {
  const previous = readLocalResume();
  const clearedSessionIds = Array.from(new Set([...previous.clearedSessionIds, sessionId]));
  return saveLocalResume({
    lastSessionId: previous.lastSessionId === sessionId ? null : previous.lastSessionId,
    draftInput: "",
    lastKnownStatus: "EMPTY",
    clearedSessionIds
  });
}
