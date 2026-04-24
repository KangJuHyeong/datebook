import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { apiRequestJson, ApiError } from "../src/lib/api/runtime.mjs";
import { resolveProtectedRoute, resolveRootRoute } from "../src/lib/routing/guards.mjs";

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
    new Response(JSON.stringify({ code: "AUTH_REQUIRED", message: "로그인이 필요합니다.", fields: [] }), {
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
    new Response(JSON.stringify({ code: "EXPORT_STATE_INVALID", message: "상태를 다시 확인해주세요.", fields: [] }), {
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
  const user = { id: 1, email: "a@example.com", displayName: "민지", coupleId: 10 };
  assert.deepEqual(resolveProtectedRoute(user), { allowed: true, user });
});

console.log("frontend tests passed");
