import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import {
  cancelExportSelection,
  getExportEntryViewModel,
  submitExportCompletion,
  submitExportSelection,
  toggleExportSelection,
} from "../src/features/export/export-flow-logic.mjs";

async function runTest(name, fn) {
  try {
    await fn();
    console.log(`PASS ${name}`);
  } catch (error) {
    console.error(`FAIL ${name}`);
    throw error;
  }
}

await runTest("export selection toggles only exportable entries", async () => {
  assert.deepEqual(toggleExportSelection([], 100, true), [100]);
  assert.deepEqual(toggleExportSelection([100], 100, true), []);
  assert.deepEqual(toggleExportSelection([100], 101, false), [100]);
});

await runTest("export entry view model marks locked records as unavailable", async () => {
  const exportable = getExportEntryViewModel({
    date: "2026-04-22",
    myAnswerStatus: "ANSWERED",
    partnerAnswerStatus: "REVEALED",
    exportable: true,
  });
  const locked = getExportEntryViewModel({
    date: "2026-04-21",
    myAnswerStatus: "ANSWERED",
    partnerAnswerStatus: "LOCKED",
    exportable: false,
  });

  assert.equal(exportable.availabilityBadge, "선택 가능");
  assert.equal(exportable.partnerAnswerBadge, "공개됨");
  assert.equal(locked.availabilityBadge, "잠금 기록");
  assert.equal(locked.availabilityDescription, "둘 다 답한 기록만 주문할 수 있어요.");
});

await runTest("submitExportSelection validates empty selection and returns preview flow", async () => {
  const emptyResult = await submitExportSelection([], {
    createExportOrder: async () => {
      throw new Error("should not be called");
    },
    previewExportOrder: async () => {
      throw new Error("should not be called");
    },
  });

  assert.deepEqual(emptyResult, {
    ok: false,
    message: "선택한 기록이 없어요. 주문할 기록을 골라주세요.",
  });

  const calls = [];
  const successResult = await submitExportSelection([100, 101], {
    createExportOrder: async (payload) => {
      calls.push({ type: "create", payload });
      return { exportRequestId: 33, status: "REQUESTED", itemCount: 2 };
    },
    previewExportOrder: async (exportRequestId) => {
      calls.push({ type: "preview", exportRequestId });
      return {
        exportRequestId,
        status: "PREVIEWED",
        entries: [{ date: "2026-04-22", question: "질문", answers: [{ displayName: "민지", content: "답변" }] }],
      };
    },
  });

  assert.deepEqual(calls, [
    { type: "create", payload: { dailyQuestionIds: [100, 101] } },
    { type: "preview", exportRequestId: 33 },
  ]);
  assert.deepEqual(successResult, {
    ok: true,
    exportRequestId: 33,
    preview: {
      exportRequestId: 33,
      status: "PREVIEWED",
      entries: [{ date: "2026-04-22", question: "질문", answers: [{ displayName: "민지", content: "답변" }] }],
    },
  });
});

await runTest("submitExportCompletion and cancelExportSelection surface API state", async () => {
  const completion = await submitExportCompletion(33, {
    completeExportOrder: async (exportRequestId) => ({
      exportRequestId,
      status: "COMPLETED",
      completedAt: "2026-04-22T10:00:00",
      downloads: [
        { format: "json", url: "/api/exports/33/download?format=json" },
        { format: "text", url: "/api/exports/33/download?format=text" },
      ],
    }),
  });

  assert.deepEqual(completion, {
    ok: true,
    completed: {
      exportRequestId: 33,
      status: "COMPLETED",
      completedAt: "2026-04-22T10:00:00",
      downloads: [
        { format: "json", url: "/api/exports/33/download?format=json" },
        { format: "text", url: "/api/exports/33/download?format=text" },
      ],
    },
  });

  const cancelled = await cancelExportSelection(33, {
    cancelExportOrder: async () => ({ exportRequestId: 33, status: "CANCELLED" }),
  });

  assert.deepEqual(cancelled, { ok: true });
});

await runTest("export client source supports disabled locked rows and completion-only downloads", async () => {
  const source = await readFile(new URL("../src/app/export/export-client.tsx", import.meta.url), "utf8");
  assert.match(source, /role="checkbox"/);
  assert.match(source, /aria-disabled=\{!entry\.exportable\}/);
  assert.match(source, /event\.key === " " \|\| event\.key === "Spacebar"/);
  assert.match(source, /flowStep === "completed" && completedOrder && preview/);
  assert.match(source, /JSON 다운로드/);
  assert.match(source, /텍스트 다운로드/);
});

await runTest("order-facing page copy does not use placeholder text", async () => {
  const pageSource = await readFile(new URL("../src/app/export/page.tsx", import.meta.url), "utf8");
  assert.match(pageSource, /title="주문"/);
  assert.match(pageSource, /주문 미리보기/);
  assert.doesNotMatch(pageSource, /PlaceholderPanel/);
});

console.log("frontend export tests passed");
