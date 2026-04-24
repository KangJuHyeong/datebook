import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { resolveAuthRoute, resolveCoupleRoute, resolveProtectedRoute, resolveRootRoute } from "./guards.mjs";
import type { AuthUser } from "@/types/api";
import { ApiError, apiRequestJson } from "@/lib/api/client";

export async function getCurrentUserOnServer(): Promise<AuthUser | null> {
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
  const user = await getCurrentUserOnServer();
  return resolveRootRoute(user);
}

export async function requireProtectedUser() {
  const user = await getCurrentUserOnServer();
  const result = resolveProtectedRoute(user);

  if (!result.allowed) {
    redirect(result.redirectTo);
  }

  return result.user;
}

export async function redirectAuthenticatedUser() {
  const user = await getCurrentUserOnServer();
  const redirectTo = resolveAuthRoute(user);

  if (redirectTo) {
    redirect(redirectTo);
  }
}

export async function requireCoupleSetupUser() {
  const user = await getCurrentUserOnServer();
  const redirectTo = resolveCoupleRoute(user);

  if (redirectTo) {
    redirect(redirectTo);
  }

  return user as AuthUser;
}
