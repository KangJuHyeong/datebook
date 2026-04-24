import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { resolveProtectedRoute, resolveRootRoute } from "./guards.mjs";
import type { AuthUser } from "@/types/api";
import { ApiError, apiRequestJson } from "@/lib/api/client";

async function fetchCurrentUserFromServer(): Promise<AuthUser | null> {
  const cookieHeader = (await cookies()).toString();

  try {
    return await apiRequestJson<AuthUser>("/api/auth/me", {
      method: "GET",
      headers: cookieHeader ? { Cookie: cookieHeader } : {},
      cache: "no-store",
      next: { revalidate: 0 },
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }

    return null;
  }
}

export async function getRootRedirectPath() {
  const user = await fetchCurrentUserFromServer();
  return resolveRootRoute(user);
}

export async function requireProtectedUser() {
  const user = await fetchCurrentUserFromServer();
  const result = resolveProtectedRoute(user);

  if (!result.allowed) {
    redirect(result.redirectTo);
  }

  return result.user;
}
