const BACKEND_API_BASE_URL =
  process.env.API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
const UNSAFE_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

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

function isUnsafeMethod(method = "GET") {
  return UNSAFE_METHODS.has(method.toUpperCase());
}

function readCookie(name) {
  if (typeof document === "undefined") {
    return null;
  }

  const encodedName = `${encodeURIComponent(name)}=`;
  const cookie = document.cookie
    .split("; ")
    .find((part) => part.startsWith(encodedName));

  return cookie ? decodeURIComponent(cookie.slice(encodedName.length)) : null;
}

async function fetchCsrfToken(customFetch) {
  const response = await customFetch(buildApiUrl("/api/auth/csrf"), {
    method: "GET",
    credentials: "include",
    headers: {
      Accept: "application/json",
    },
  });
  const text = await response.text();
  const payload = parseJsonOrNull(text);

  if (!response.ok) {
    const error = parseApiErrorPayload(payload, response.status);
    throw new ApiError(response.status, error, getErrorHandlingOptions(response.status, error.code));
  }

  return payload?.token ?? null;
}

async function ensureCsrfHeader(headers, method, customFetch) {
  if (headers.has(CSRF_HEADER_NAME) || !isUnsafeMethod(method) || typeof window === "undefined") {
    return;
  }

  const token = await fetchCsrfToken(customFetch) ?? readCookie(CSRF_COOKIE_NAME);

  if (token) {
    headers.set(CSRF_HEADER_NAME, token);
  }
}

function parseJsonOrNull(text) {
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
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
  const method = init.method ?? "GET";
  const requestInit = {
    ...init,
    credentials: "include",
    headers,
  };

  if (requestInit.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  await ensureCsrfHeader(headers, method, customFetch);

  return customFetch(buildApiUrl(path), requestInit);
}

export async function apiRequestJson(path, init = {}, customFetch = fetch) {
  const response = await apiFetch(path, init, customFetch);
  const text = await response.text();
  const payload = parseJsonOrNull(text);

  if (!response.ok) {
    const error = parseApiErrorPayload(payload, response.status);
    throw new ApiError(response.status, error, getErrorHandlingOptions(response.status, error.code));
  }

  return payload;
}
