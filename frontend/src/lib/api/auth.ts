import { apiRequestJson } from "./client";
import type {
  AuthUser,
  LoginRequest,
  LogoutResponse,
  SignupRequest,
  SignupResponse,
} from "@/types/api";

export function getCurrentUser() {
  return apiRequestJson<AuthUser>("/api/auth/me", { method: "GET" });
}

export function signup(data: SignupRequest) {
  return apiRequestJson<SignupResponse>("/api/auth/signup", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export function login(data: LoginRequest) {
  return apiRequestJson<AuthUser>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

export function logout() {
  return apiRequestJson<LogoutResponse>("/api/auth/logout", {
    method: "POST",
  });
}
