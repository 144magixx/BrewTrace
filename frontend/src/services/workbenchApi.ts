import type { ApiError, ApiResponse } from "./apiClient";
import type { ClearSessionRequest, CreateSessionRequest, SubmitMessageRequest, WebConversationMessage, WorkbenchSnapshot } from "./workbenchTypes";

const DEFAULT_REQUEST_TIMEOUT_MS = 15_000;
const MODEL_REQUEST_TIMEOUT_MS = 125_000;

type WorkbenchRequestInit = RequestInit & {
  timeoutMs?: number;
};

export const SENSITIVE_PATTERNS = [
  /sk-[A-Za-z0-9_-]{20,}/g,
  /Authorization\s*[:=]\s*Bearer\s+[A-Za-z0-9._-]+/gi,
  /Bearer\s+[A-Za-z0-9._-]{12,}/gi,
  /Cookie\s*[:=]\s*[^;\n,}"]+/gi,
  /Session[-_ ]?Token\s*[:=]\s*[A-Za-z0-9._-]+/gi,
  /OPENAI_API_KEY\s*=\s*[^\s\n]+/gi
];

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
    timeoutMs: MODEL_REQUEST_TIMEOUT_MS,
    body: JSON.stringify(payload)
  } as WorkbenchRequestInit);
}

export type SubmitMessageStreamHandlers = {
  onUserMessage?: (message: WebConversationMessage) => void;
  onAssistantStart?: (id: string) => void;
  onAssistantDelta?: (id: string, delta: string) => void;
  onSnapshot?: (snapshot: WorkbenchSnapshot) => void;
  onDone?: () => void;
};

export async function submitMessageStream(sessionId: string, payload: SubmitMessageRequest, handlers: SubmitMessageStreamHandlers): Promise<void> {
  const response = await fetch(`/api/workbench/sessions/${encodeURIComponent(sessionId)}/messages/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Request-Id": requestId()
    },
    body: JSON.stringify(payload)
  });
  if (!response.ok) {
    throw new WorkbenchApiError({
      code: "HTTP_ERROR",
      category: "RETRYABLE",
      message: "流式提交失败，已保留你的输入。",
      recoverable: true,
      nextActions: ["RETRY"],
      details: { status: response.status, statusText: response.statusText }
    });
  }
  const contentType = response.headers.get("Content-Type") ?? "";
  if (!contentType.includes("text/event-stream")) {
    const envelope = parseApiResponse<WorkbenchSnapshot>(await response.text(), response.status, response.statusText);
    if (envelope.error || envelope.data === null) {
      throw new WorkbenchApiError(envelope.error ?? fallbackStreamError());
    }
    handlers.onSnapshot?.(envelope.data);
    handlers.onDone?.();
    return;
  }
  await readSseStream(response, handlers);
}

export async function clearSession(sessionId: string, payload: ClearSessionRequest): Promise<WorkbenchSnapshot> {
  return request<WorkbenchSnapshot>(`/api/workbench/sessions/${encodeURIComponent(sessionId)}/clear`, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let envelope: ApiResponse<T>;
  let ok: boolean;
  try {
    const transport = await sendRequest<T>(path, init);
    envelope = transport.envelope;
    ok = transport.ok;
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

  if (!ok || envelope.error || envelope.data === null) {
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

async function sendRequest<T>(path: string, init: RequestInit): Promise<{ ok: boolean; envelope: ApiResponse<T> }> {
  const timeoutMs = (init as WorkbenchRequestInit).timeoutMs ?? DEFAULT_REQUEST_TIMEOUT_MS;
  const headers = {
    "Content-Type": "application/json",
    "X-Request-Id": requestId(),
    ...(init.headers ?? {})
  } as Record<string, string>;
  if (typeof fetch === "function") {
    const controller = typeof AbortController === "function" ? new AbortController() : null;
    const timeoutId = controller ? setTimeout(() => controller.abort(), timeoutMs) : null;
    try {
      const response = await fetch(path, {
        ...init,
        headers,
        signal: controller?.signal
      });
      return { ok: response.ok, envelope: parseApiResponse<T>(await response.text(), response.status, response.statusText) };
    } finally {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    }
  }
  return xhrRequest<T>(path, init, headers, timeoutMs);
}

function parseApiResponse<T>(text: string, status: number, statusText: string): ApiResponse<T> {
  try {
    return JSON.parse(text) as ApiResponse<T>;
  } catch {
    return {
      requestId: "unknown",
      data: null,
      error: {
        code: "HTTP_NON_JSON_RESPONSE",
        category: "RETRYABLE",
        message: status >= 200 && status < 300 ? "接口返回格式异常，请重试。" : "请求失败，后端返回了非 JSON 响应。",
        recoverable: true,
        nextActions: ["CHECK_LOCAL_SERVICE", "RETRY"],
        details: {
          status,
          statusText,
          responseText: text.slice(0, 300)
        }
      }
    };
  }
}

async function readSseStream(response: Response, handlers: SubmitMessageStreamHandlers): Promise<void> {
  if (!response.body) {
    throw new WorkbenchApiError(fallbackStreamError());
  }
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  while (true) {
    const { value, done } = await reader.read();
    buffer += decoder.decode(value ?? new Uint8Array(), { stream: !done });
    const blocks = buffer.split(/\n\n/);
    buffer = blocks.pop() ?? "";
    for (const block of blocks) {
      handleSseBlock(block, handlers);
    }
    if (done) {
      if (buffer.trim().length > 0) {
        handleSseBlock(buffer, handlers);
      }
      return;
    }
  }
}

function handleSseBlock(block: string, handlers: SubmitMessageStreamHandlers) {
  const lines = block.split(/\n/);
  const event = lines.find((line) => line.startsWith("event:"))?.slice("event:".length).trim() ?? "message";
  const data = lines
    .filter((line) => line.startsWith("data:"))
    .map((line) => line.slice("data:".length).trimStart())
    .join("\n");
  const payload = data ? JSON.parse(data) as Record<string, unknown> : {};
  if (event === "user_message" && isRecord(payload.message)) {
    handlers.onUserMessage?.(payload.message as WebConversationMessage);
  }
  if (event === "assistant_start" && typeof payload.id === "string") {
    handlers.onAssistantStart?.(payload.id);
  }
  if (event === "assistant_delta" && typeof payload.id === "string" && typeof payload.delta === "string") {
    handlers.onAssistantDelta?.(payload.id, payload.delta);
  }
  if (event === "snapshot" && isRecord(payload.snapshot)) {
    handlers.onSnapshot?.(payload.snapshot as WorkbenchSnapshot);
  }
  if (event === "done") {
    handlers.onDone?.();
  }
  if (event === "error" && isRecord(payload.error)) {
    throw new WorkbenchApiError(payload.error as ApiError);
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function fallbackStreamError(): ApiError {
  return {
    code: "STREAM_FAILED",
    category: "RETRYABLE",
    message: "流式提交失败，已保留你的输入。",
    recoverable: true,
    nextActions: ["RETRY"],
    details: {}
  };
}

function xhrRequest<T>(path: string, init: RequestInit, headers: Record<string, string>, timeoutMs: number): Promise<{ ok: boolean; envelope: ApiResponse<T> }> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(init.method ?? "GET", path);
    xhr.timeout = timeoutMs;
    for (const [key, value] of Object.entries(headers)) {
      xhr.setRequestHeader(key, value);
    }
    xhr.onload = () => {
      resolve({
        ok: xhr.status >= 200 && xhr.status < 300,
        envelope: parseApiResponse<T>(xhr.responseText, xhr.status, xhr.statusText)
      });
    };
    xhr.onerror = () => reject(new Error("Network error"));
    xhr.ontimeout = () => reject(new Error("Request timeout"));
    xhr.send(typeof init.body === "string" ? init.body : null);
  });
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

export function redactSensitiveText(value: string): string {
  return SENSITIVE_PATTERNS.reduce((text, pattern) => text.replace(pattern, "[REDACTED]"), value);
}

function requestId(): string {
  return globalThis.crypto?.randomUUID?.() ?? `req-${Date.now()}`;
}
