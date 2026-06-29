export type ApiError = {
  code: string;
  category: "USER_FIXABLE" | "RETRYABLE" | "DEGRADED" | "FATAL" | "SAFETY_BLOCKED";
  message: string;
  recoverable: boolean;
  nextActions: string[];
  details?: Record<string, unknown>;
};

export type ApiResponse<T> = {
  requestId: string;
  data: T | null;
  error: ApiError | null;
};

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  if (response.error || response.data === null) {
    throw new Error(response.error?.message ?? "接口返回为空");
  }
  return response.data;
}
