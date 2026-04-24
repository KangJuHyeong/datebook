import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { apiRequestJson, ApiError } from "../src/lib/api/runtime.mjs";
import { resolveAuthRoute, resolveCoupleRoute, resolveProtectedRoute, resolveRootRoute } from "../src/lib/routing/guards.mjs";
import {
  getPostAuthRedirectPath,
  submitLogin,
  submitSignup,
  validateLoginValues,
  validateSignupValues,
} from "../src/features/auth/auth-form-logic.mjs";
import { submitCreateInvite, submitJoinInvite, validateInviteCode } from "../src/features/couple/couple-form-logic.mjs";

async function runTest(name, fn) {
  try {
    await fn();
    console.log(`PASS ${name}`);
  } catch (error) {
    console.error(`FAIL ${name}`);
    throw error;
  }
}

async function expectRejects(factory, assertion) {
  let error;

  try {
    await factory();
  } catch (caught) {
    error = caught;
  }

  assert.ok(error, "Expected promise to reject.");
  assertion(error);
}

await runTest("api client source keeps credentials include by default", async () => {
  const source = await readFile(new URL("../src/lib/api/client.ts", import.meta.url), "utf8");
  assert.match(source, /credentials:\s*"include"/);
});

await runTest("apiRequestJson sends credentials include and parses success payload", async () => {
  const calls = [];
  const mockFetch = async (url, init) => {
    calls.push({ url, init });
    return new Response(JSON.stringify({ ok: true }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  };

  const payload = await apiRequestJson("/api/auth/me", { method: "GET" }, mockFetch);

  assert.deepEqual(payload, { ok: true });
  assert.equal(calls[0].init.credentials, "include");
  assert.equal(calls[0].url, "http://localhost:8080/api/auth/me");
});

await runTest("apiRequestJson surfaces 401 auth errors with redirect info", async () => {
  const mockFetch = async () =>
    new Response(JSON.stringify({ code: "AUTH_REQUIRED", message: "\ub85c\uadf8\uc778\uc774 \ud544\uc694\ud569\ub2c8\ub2e4.", fields: [] }), {
      status: 401,
      headers: { "Content-Type": "application/json" },
    });

  await expectRejects(
    () => apiRequestJson("/api/auth/me", { method: "GET" }, mockFetch),
    (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 401);
      assert.equal(error.code, "AUTH_REQUIRED");
      assert.equal(error.redirectTo, "/login");
    },
  );
});

await runTest("apiRequestJson surfaces 409 conflicts with refetch hint", async () => {
  const mockFetch = async () =>
    new Response(JSON.stringify({ code: "EXPORT_STATE_INVALID", message: "\uc0c1\ud0dc\ub97c \ub2e4\uc2dc \ud655\uc778\ud574\uc8fc\uc138\uc694.", fields: [] }), {
      status: 409,
      headers: { "Content-Type": "application/json" },
    });

  await expectRejects(
    () => apiRequestJson("/api/exports/3/complete", { method: "POST" }, mockFetch),
    (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 409);
      assert.equal(error.code, "EXPORT_STATE_INVALID");
      assert.equal(error.shouldRefetch, true);
    },
  );
});

await runTest("resolveRootRoute sends unauthenticated users to login", async () => {
  assert.equal(resolveRootRoute(null), "/login");
});

await runTest("resolveRootRoute sends logged-in users without couple to couple page", async () => {
  assert.equal(resolveRootRoute({ id: 1, coupleId: null }), "/couple");
});

await runTest("resolveRootRoute sends fully connected users to today page", async () => {
  assert.equal(resolveRootRoute({ id: 1, coupleId: 10 }), "/today");
});

await runTest("resolveProtectedRoute blocks unauthenticated users", async () => {
  assert.deepEqual(resolveProtectedRoute(null), {
    allowed: false,
    redirectTo: "/login",
  });
});

await runTest("resolveProtectedRoute blocks users without a couple", async () => {
  assert.deepEqual(resolveProtectedRoute({ id: 1, coupleId: null }), {
    allowed: false,
    redirectTo: "/couple",
  });
});

await runTest("resolveProtectedRoute allows connected users", async () => {
  const user = { id: 1, email: "a@example.com", displayName: "\ubbfc\uc9c0", coupleId: 10 };
  assert.deepEqual(resolveProtectedRoute(user), { allowed: true, user });
});

await runTest("resolveAuthRoute redirects logged-in users away from auth pages", async () => {
  assert.equal(resolveAuthRoute({ id: 1, coupleId: 12 }), "/today");
  assert.equal(resolveAuthRoute({ id: 1, coupleId: null }), "/couple");
  assert.equal(resolveAuthRoute(null), null);
});

await runTest("resolveCoupleRoute handles unauthenticated and connected users", async () => {
  assert.equal(resolveCoupleRoute(null), "/login");
  assert.equal(resolveCoupleRoute({ id: 1, coupleId: 12 }), "/today");
  assert.equal(resolveCoupleRoute({ id: 1, coupleId: null }), null);
});

await runTest("validateLoginValues reports missing fields and invalid email", async () => {
  assert.deepEqual(validateLoginValues({ email: "wrong", password: "" }), {
    email: "\uc62c\ubc14\ub978 \uc774\uba54\uc77c \ud615\uc2dd\uc774 \uc544\ub2d9\ub2c8\ub2e4.",
    password: "\ube44\ubc00\ubc88\ud638\ub97c \uc785\ub825\ud574\uc8fc\uc138\uc694.",
  });
});

await runTest("validateSignupValues enforces display name and password length", async () => {
  assert.deepEqual(validateSignupValues({ email: "a@example.com", password: "short", displayName: "" }), {
    displayName: "\ud45c\uc2dc \uc774\ub984\uc744 \uc785\ub825\ud574\uc8fc\uc138\uc694.",
    password: "\ube44\ubc00\ubc88\ud638\ub294 8\uc790 \uc774\uc0c1 72\uc790 \uc774\ud558\ub85c \uc785\ub825\ud574\uc8fc\uc138\uc694.",
  });
});

await runTest("submitLogin redirects users without a couple to couple page", async () => {
  const result = await submitLogin(
    { email: "a@example.com", password: "password123" },
    {
      login: async () => ({ id: 1, email: "a@example.com", displayName: "\ubbfc\uc9c0", coupleId: null }),
    },
  );

  assert.deepEqual(result, { ok: true, redirectTo: "/couple" });
});

await runTest("submitLogin returns generic login failure message", async () => {
  const result = await submitLogin(
    { email: "a@example.com", password: "password123" },
    {
      login: async () => {
        throw new ApiError(401, { code: "LOGIN_FAILED", message: "\uc774\uba54\uc77c \ub610\ub294 \ube44\ubc00\ubc88\ud638\uac00 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.", fields: [] });
      },
    },
  );

  assert.deepEqual(result, {
    ok: false,
    fieldErrors: {},
    formError: "\uc774\uba54\uc77c \ub610\ub294 \ube44\ubc00\ubc88\ud638\uac00 \uc62c\ubc14\ub974\uc9c0 \uc54a\uc2b5\ub2c8\ub2e4.",
  });
});

await runTest("submitSignup loads current user and redirects after success", async () => {
  const result = await submitSignup(
    { email: "a@example.com", password: "password123", displayName: "\ubbfc\uc9c0" },
    {
      signup: async () => ({ id: 1 }),
      getCurrentUser: async () => ({ id: 1, email: "a@example.com", displayName: "\ubbfc\uc9c0", coupleId: 99 }),
    },
  );

  assert.deepEqual(result, { ok: true, redirectTo: "/today" });
});

await runTest("submitSignup surfaces backend validation fields", async () => {
  const result = await submitSignup(
    { email: "dup@example.com", password: "password123", displayName: "\ubbfc\uc9c0" },
    {
      signup: async () => {
        throw new ApiError(400, {
          code: "VALIDATION_ERROR",
          message: "\uc785\ub825\uac12\uc744 \ud655\uc778\ud574\uc8fc\uc138\uc694.",
          fields: [{ field: "email", message: "\uc774\ubbf8 \uc0ac\uc6a9 \uc911\uc778 \uc774\uba54\uc77c\uc785\ub2c8\ub2e4." }],
        });
      },
      getCurrentUser: async () => {
        throw new Error("should not be called");
      },
    },
  );

  assert.deepEqual(result, {
    ok: false,
    fieldErrors: { email: "\uc774\ubbf8 \uc0ac\uc6a9 \uc911\uc778 \uc774\uba54\uc77c\uc785\ub2c8\ub2e4." },
    formError: undefined,
  });
});

await runTest("getPostAuthRedirectPath sends coupled users to today", async () => {
  assert.equal(getPostAuthRedirectPath({ coupleId: 1 }), "/today");
  assert.equal(getPostAuthRedirectPath({ coupleId: null }), "/couple");
});

await runTest("validateInviteCode requires a code", async () => {
  assert.deepEqual(validateInviteCode({ inviteCode: "   " }), {
    inviteCode: "\ucd08\ub300 \ucf54\ub4dc\ub97c \uc785\ub825\ud574\uc8fc\uc138\uc694.",
  });
});

await runTest("submitCreateInvite returns invite data from mock API", async () => {
  const result = await submitCreateInvite({
    createCouple: async () => ({ coupleId: 10, inviteCode: "A1B2C3D4", expiresAt: "2026-04-23T09:00:00" }),
  });

  assert.deepEqual(result, {
    ok: true,
    invite: { coupleId: 10, inviteCode: "A1B2C3D4", expiresAt: "2026-04-23T09:00:00" },
  });
});

await runTest("submitJoinInvite redirects to today on success", async () => {
  const result = await submitJoinInvite(
    { inviteCode: " A1B2C3D4 " },
    {
      joinCouple: async (data) => {
        assert.equal(data.inviteCode, "A1B2C3D4");
        return { coupleId: 10, memberCount: 2 };
      },
    },
  );

  assert.deepEqual(result, {
    ok: true,
    response: { coupleId: 10, memberCount: 2 },
    redirectTo: "/today",
  });
});

await runTest("submitJoinInvite surfaces invite errors from mock API", async () => {
  const result = await submitJoinInvite(
    { inviteCode: "BADCODE" },
    {
      joinCouple: async () => {
        throw new ApiError(400, {
          code: "INVITE_CODE_INVALID",
          message: "\ucd08\ub300 \ucf54\ub4dc\ub97c \ub2e4\uc2dc \ud655\uc778\ud574\uc8fc\uc138\uc694.",
          fields: [],
        });
      },
    },
  );

  assert.deepEqual(result, {
    ok: false,
    fieldErrors: {},
    formError: "\ucd08\ub300 \ucf54\ub4dc\ub97c \ub2e4\uc2dc \ud655\uc778\ud574\uc8fc\uc138\uc694.",
  });
});

console.log("frontend tests passed");
