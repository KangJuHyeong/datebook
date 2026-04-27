const BACKEND_API_BASE_URL =
  process.env.API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export function getApiBaseUrl() {
  return typeof window === "undefined" ? BACKEND_API_BASE_URL : "";
}

function normalizeHeaders(headers = {}) {
  const normalized = new Headers(headers);

  if (!normalized.has("Accept")) {
    normalized.set("Accept", "application/json");
  }

  return normalized;
}

export class ApiError extends Error {
  constructor(status, error, options = {}) {
    super(error.message);
    this.name = "ApiError";
    this.status = status;
    this.code = error.code;
    this.fields = error.fields ?? [];
    this.redirectTo = options.redirectTo;
    this.shouldRefetch = options.shouldRefetch ?? false;
  }
}

export function buildApiUrl(path) {
  return `${getApiBaseUrl()}${path}`;
}

export function parseApiErrorPayload(payload, fallbackStatus = 500) {
  if (payload && typeof payload === "object" && typeof payload.code === "string" && typeof payload.message === "string") {
    return {
      code: payload.code,
      message: payload.message,
      fields: Array.isArray(payload.fields) ? payload.fields : [],
    };
  }

  return {
    code: fallbackStatus >= 500 ? "INTERNAL_ERROR" : "INVALID_ERROR_RESPONSE",
    message: fallbackStatus >= 500 ? "잠시 후 다시 시도해주세요." : "요청을 처리하지 못했습니다.",
    fields: [],
  };
}

export function getErrorHandlingOptions(status, errorCode) {
  if (status === 401 && errorCode === "AUTH_REQUIRED") {
    return { redirectTo: "/login", shouldRefetch: false };
  }

  if (status === 409) {
    return { shouldRefetch: true };
  }

  return { shouldRefetch: false };
}

export async function apiFetch(path, init = {}, customFetch = fetch) {
  const headers = normalizeHeaders(init.headers);
  const requestInit = {
    ...init,
    credentials: "include",
    headers,
  };

  if (requestInit.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  return customFetch(buildApiUrl(path), requestInit);
}

export async function apiRequestJson(path, init = {}, customFetch = fetch) {
  const response = await apiFetch(path, init, customFetch);
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const error = parseApiErrorPayload(payload, response.status);
    throw new ApiError(response.status, error, getErrorHandlingOptions(response.status, error.code));
  }

  return payload;
}
