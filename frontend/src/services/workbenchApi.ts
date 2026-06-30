import type { ApiError, ApiResponse } from "./apiClient";
import type { CreateSessionRequest, SubmitMessageRequest, WorkbenchSnapshot } from "./workbenchTypes";

const SENSITIVE_PATTERNS = [/sk-[A-Za-z0-9_-]{20,}/g, /Authorization:\s*Bearer\s+[A-Za-z0-9._-]+/gi, /Cookie:\s*[^;]+/gi, /Session Token[:=]\s*[A-Za-z0-9._-]+/gi];

export class WorkbenchApiError extends Error {
  readonly apiError: ApiError;

  constructor(apiError: ApiError) {
    super(apiError.message);
    this.apiError = sanitizeApiError(apiError);
  }
}

export async function fetchSnapshot(sessionId?: string | null): Promise<WorkbenchSnapshot> {
  const query = sessionId ? `?sessionId=${encodeURIComponent(sessionId)}` : "";
  return request<WorkbenchSnapshot>(`/api/workbench/snapshot${query}`);
}

export async function createSession(payload: CreateSessionRequest = { mode: "EXPLICIT_WORKFLOW" }): Promise<WorkbenchSnapshot> {
  return request<WorkbenchSnapshot>("/api/workbench/sessions", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function submitMessage(sessionId: string, payload: SubmitMessageRequest): Promise<WorkbenchSnapshot> {
  return request<WorkbenchSnapshot>(`/api/workbench/sessions/${encodeURIComponent(sessionId)}/messages`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        "X-Request-Id": requestId(),
        ...(init.headers ?? {})
      }
    });
  } catch {
    throw new WorkbenchApiError({
      code: "SERVICE_UNAVAILABLE",
      category: "RETRYABLE",
      message: "本地服务暂时不可用，已保留你的输入。",
      recoverable: true,
      nextActions: ["CHECK_LOCAL_SERVICE", "RETRY"],
      details: {}
    });
  }

  const envelope = (await response.json()) as ApiResponse<T>;
  if (!response.ok || envelope.error || envelope.data === null) {
    throw new WorkbenchApiError(envelope.error ?? {
      code: "HTTP_ERROR",
      category: "RETRYABLE",
      message: "请求失败，已保留你的输入。",
      recoverable: true,
      nextActions: ["RETRY"],
      details: {}
    });
  }
  return envelope.data;
}

function sanitizeApiError(apiError: ApiError): ApiError {
  const details = apiError.details ? JSON.parse(JSON.stringify(apiError.details)) as Record<string, unknown> : {};
  for (const [key, value] of Object.entries(details)) {
    if (typeof value === "string") {
      details[key] = SENSITIVE_PATTERNS.reduce((text, pattern) => text.replace(pattern, "[REDACTED]"), value);
    }
  }
  return { ...apiError, details };
}

function requestId(): string {
  return globalThis.crypto?.randomUUID?.() ?? `req-${Date.now()}`;
}
