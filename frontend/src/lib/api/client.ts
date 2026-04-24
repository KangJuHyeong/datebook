import {
  ApiError,
  apiFetch as runtimeApiFetch,
  apiRequestJson as runtimeApiRequestJson,
} from "./runtime.mjs";

export type { ApiErrorResponse, ApiFieldError } from "@/types/api";
export { ApiError };

export async function apiFetch(path: string, init: RequestInit = {}) {
  return runtimeApiFetch(path, {
    ...init,
    credentials: "include",
  });
}

export async function apiRequestJson<T>(path: string, init: RequestInit = {}) {
  return runtimeApiRequestJson(path, {
    ...init,
    credentials: "include",
  }) as Promise<T>;
}
