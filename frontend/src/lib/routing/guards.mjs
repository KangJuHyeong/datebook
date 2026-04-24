export function resolveRootRoute(user) {
  if (!user) {
    return "/login";
  }

  return user.coupleId ? "/today" : "/couple";
}

export function resolveProtectedRoute(user) {
  if (!user) {
    return { allowed: false, redirectTo: "/login" };
  }

  if (!user.coupleId) {
    return { allowed: false, redirectTo: "/couple" };
  }

  return { allowed: true, user };
}
