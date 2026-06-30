import type { WorkbenchStatus } from "../services/workbenchTypes";

const STORAGE_KEY = "coffee-agent.local-resume";

export type LocalResumeState = {
  lastSessionId: string | null;
  draftInput: string;
  lastKnownStatus: WorkbenchStatus | "UNKNOWN";
  savedAt: string;
};

export function readLocalResume(): LocalResumeState {
  const fallback: LocalResumeState = { lastSessionId: null, draftInput: "", lastKnownStatus: "UNKNOWN", savedAt: new Date(0).toISOString() };
  if (typeof localStorage === "undefined") {
    return fallback;
  }
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return fallback;
  }
  try {
    return { ...fallback, ...JSON.parse(raw) } as LocalResumeState;
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
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  return next;
}

export function clearLocalResume(): void {
  localStorage.removeItem(STORAGE_KEY);
}
