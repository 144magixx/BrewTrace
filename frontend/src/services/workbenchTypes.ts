import type { ApiError } from "./apiClient";

export type OrchestrationMode = "EXPLICIT_WORKFLOW" | "MODEL_TOOL_CALLING";
export type WorkbenchStatus = "EMPTY" | "SESSION_CREATED" | "WAITING_FOR_FACTS" | "DRAFTS_READY" | "ERROR_RECOVERABLE";
export type SourceType = "USER_CONFIRMED" | "MODEL_SUGGESTED" | "TOOL_OUTPUT" | "EXTERNAL_REFERENCE" | "SYSTEM_INFERRED";

export type CreateSessionRequest = {
  mode: OrchestrationMode;
};

export type SubmitMessageRequest = {
  content: string;
  modelMode?: ModelModeCode;
};

export type ClearSessionRequest = {
  confirmed: boolean;
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
  agentState: AgentStateSnapshot;
  lastError: ApiError | null;
  updatedAt: string;
};

export type SendStatus = "WILL_SEND" | "PAGE_ONLY" | "SEND_AFTER_CONFIRMATION" | "EXCLUDED";
export type RiskLevel = "NONE" | "INFO" | "WARNING" | "HIGH";
export type AgentCardType =
  | "SESSION_CONTEXT"
  | "CONFIRMED_FACT"
  | "PENDING_ASSOCIATION"
  | "CANDIDATE_MEMORY"
  | "CONTEXT_PREVIEW"
  | "MODEL_OUTPUT"
  | "FACT_BOUNDARY_CHECK"
  | "CAPABILITY_BOUNDARY"
  | "SESSION_CONTROL";
export type BasisType = "USER_CONFIRMED" | "MODEL_INFERENCE" | "CANDIDATE_MEMORY" | "PENDING_ASSOCIATION" | "UNSUPPORTED" | "CONFLICT";
export type RecommendedAction = "KEEP" | "ASK_USER_CONFIRMATION" | "EXCLUDE_FROM_FINAL_RECORD" | "REWRITE";
export type ModelModeCode = "openai-gpt55";
export type ModelMessageType = "POST" | "CONVERSATION";

export type AgentStateSnapshot = {
  statusCards: AgentStatusCard[];
  contextItems: ContextItem[];
  confirmedFacts: ConfirmedFact[];
  pendingAssociations: PendingAssociation[];
  candidateMemories: CandidateMemory[];
  contextPreview: ContextPreview;
  modelMode: ModelModeSnapshot;
  modelOutput: ModelOutputSnapshot | null;
  factBoundaryChecks: FactBoundaryCheckResult[];
  capabilityBoundary: CapabilityBoundary;
  sessionControlAction: SessionControlAction;
  updatedAt: string;
};

export type AgentStatusCard = {
  id: string;
  type: AgentCardType;
  title: string;
  summary: string;
  sourceLabel: string;
  sendStatus: SendStatus;
  riskLevel: RiskLevel;
  createdAt: string;
};

export type ContextItem = {
  id: string;
  role: "USER" | "ASSISTANT" | "SYSTEM";
  content: string;
  sourceType: SourceType;
  confirmationStatus: "CONFIRMED" | "PENDING_CONFIRMATION" | "ACCEPTED" | "REJECTED" | "EDITED";
  sendStatus: SendStatus;
  sourceMessageId: string | null;
  createdAt: string;
};

export type ConfirmedFact = {
  id: string;
  factType: string;
  value: string;
  sourceContextItemId: string | null;
  confirmationStatus: "CONFIRMED" | "PENDING_CONFIRMATION" | "ACCEPTED" | "REJECTED" | "EDITED";
  sendStatus: SendStatus;
};

export type PendingAssociation = {
  id: string;
  value: string;
  triggerFactId: string;
  reason: string;
  confirmationStatus: "CONFIRMED" | "PENDING_CONFIRMATION" | "ACCEPTED" | "REJECTED" | "EDITED";
  sendStatus: SendStatus;
};

export type CandidateMemory = {
  id: string;
  title: string;
  content: string;
  sourceBoundary: string;
  reason: string;
  relationType: string;
  similarityLabel: string;
  conflictStatus: string;
  sendStatus: SendStatus;
};

export type ContextPreview = {
  sections: ContextPreviewSection[];
  willSendCount: number;
  excludedCount: number;
  boundaryNote: string;
  requestPreview: ModelRequestPreview | null;
  responsePreview: ModelResponsePreview | null;
};

export type ContextPreviewSection = {
  sectionType: "CURRENT_SESSION" | "CONFIRMED_FACTS" | "PENDING_ASSOCIATIONS" | "CANDIDATE_MEMORIES" | string;
  title: string;
  items: ContextPreviewItem[];
};

export type ContextPreviewItem = {
  content: string;
  sourceLabel: string;
  sendStatus: SendStatus;
  exclusionReason: string | null;
};

export type ModelOutputSnapshot = {
  outputType: "SIMULATED" | "REAL_MODEL" | "ERROR" | "FIXED_SAMPLE" | string;
  messageType: ModelMessageType | null;
  talk: string;
  mode: ModelModeCode | string;
  modelName: string | null;
  statusLabel: string;
  content: string;
  sourceBoundary: string;
  post: PostModelMessage | null;
  conversation: ConversationModelMessage | null;
  warnings: string[];
  variants: CopyVariant[];
  requestPreview: ModelRequestPreview | null;
  responsePreview: ModelResponsePreview | null;
  recoverableError: RecoverableModelError | null;
  generatedAt: string;
};

export type PostModelMessage = {
  variants: CopyVariant[];
  warnings: string[];
};

export type ConversationModelMessage = {
  questions: string[];
  answerOptions: ConversationAnswerOption[];
  pendingConfirmations: FactUsage[];
  warnings: string[];
};

export type ConversationAnswerOption = {
  id: string;
  label: string;
  content: string;
};

export type ModelModeSnapshot = {
  mode: ModelModeCode | string;
  displayName: string;
  modelName: string | null;
  baseUrlLabel: string | null;
  available: boolean;
  requiresApiKey: boolean;
  statusLabel: string;
  fallbackAvailable: boolean;
};

export type CopyVariant = {
  style: "RESTRAINED" | "EXAGGERATED" | "SHARP_REVIEW" | string;
  styleLabel: string;
  title: string;
  body: string;
  tags: string[];
  factUsages: FactUsage[];
  inferences: FactUsage[];
  pendingConfirmations: FactUsage[];
  warnings: string[];
};

export type FactUsage = {
  expression: string;
  basisType: BasisType | string;
  sourceReference: string;
  sourceId: string;
  confidenceLabel: string;
};

export type ModelRequestPreview = {
  label: string;
  modelName: string | null;
  mode: string;
  endpointPath: string;
  rawJson: string;
  redactionStatus: string;
  sentAt: string | null;
};

export type ModelResponsePreview = {
  label: string;
  modelName: string | null;
  mode: string;
  rawJson: string;
  redactionStatus: string;
  receivedAt: string | null;
};

export type RecoverableModelError = {
  code: "MODEL_TIMEOUT" | "MODEL_AUTH_FAILED" | "MODEL_RATE_LIMITED" | "MODEL_FORMAT_INVALID" | "MODEL_SERVICE_UNAVAILABLE" | string;
  category: string;
  message: string;
  recoverable: boolean;
  nextActions: string[];
  preservedSessionId: string;
  retryableMode: string;
  createdAt: string;
};

export type FactBoundaryCheckResult = {
  id: string;
  expression: string;
  basisType: BasisType;
  riskLevel: RiskLevel;
  sourceReference: string;
  message: string;
  recommendedAction: RecommendedAction;
};

export type CapabilityBoundary = {
  realModelConnected: boolean;
  longTermMemoryConnected: boolean;
  xiaohongshuConnected: boolean;
  message: string;
};

export type SessionControlAction = {
  actionType: string;
  confirmationRequired: boolean;
  impactSummary: string;
  confirmed: boolean;
  resultStatus: string;
};
