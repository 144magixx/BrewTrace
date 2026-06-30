import type { ApiError } from "./apiClient";

export type OrchestrationMode = "EXPLICIT_WORKFLOW" | "MODEL_TOOL_CALLING";
export type WorkbenchStatus = "EMPTY" | "SESSION_CREATED" | "WAITING_FOR_FACTS" | "DRAFTS_READY" | "ERROR_RECOVERABLE";
export type SourceType = "USER_CONFIRMED" | "MODEL_SUGGESTED" | "TOOL_OUTPUT" | "EXTERNAL_REFERENCE" | "SYSTEM_INFERRED";

export type CreateSessionRequest = {
  mode: OrchestrationMode;
};

export type SubmitMessageRequest = {
  content: string;
};

export type WebConversationMessage = {
  id: string;
  role: "USER" | "ASSISTANT" | "SYSTEM";
  content: string;
  sourceType: SourceType;
  createdAt: string;
};

export type RecordSummary = {
  confirmedFacts: string[];
  pendingQuestions: string[];
  suggestedFlavors: string[];
  draftStatus: "HIDDEN" | "VISIBLE" | string;
  factBoundaryNotes: string[];
};

export type DraftTab = {
  draftId: string;
  style: "RESTRAINED" | "EXAGGERATED" | "SHARP_REVIEW";
  title: string;
  body: string;
  tags: string[];
  factBoundaryNotes: string[];
  reviewWarnings: string[];
};

export type WorkbenchSnapshot = {
  sessionId: string | null;
  status: WorkbenchStatus;
  heroQuestion: string;
  orchestrationMode: OrchestrationMode;
  conversation: WebConversationMessage[];
  recordSummary: RecordSummary;
  draftTabs: DraftTab[];
  lastError: ApiError | null;
  updatedAt: string;
};
