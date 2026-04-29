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
  const selectionSource = await readFile(new URL("../src/app/export/export-selection-panel.tsx", import.meta.url), "utf8");
  const orderListSource = await readFile(new URL("../src/app/export/export-order-list.tsx", import.meta.url), "utf8");
  const completedDetailSource = await readFile(new URL("../src/app/export/export-completed-detail.tsx", import.meta.url), "utf8");
  const combinedSource = [source, selectionSource, orderListSource, completedDetailSource].join("\n");

  assert.match(selectionSource, /role="checkbox"/);
  assert.match(selectionSource, /aria-disabled=\{!entry\.exportable\}/);
  assert.match(selectionSource, /event\.key === " " \|\| event\.key === "Spacebar"/);
  assert.match(combinedSource, /주문 내역/);
  assert.match(combinedSource, /주문 예약 단계/);
  assert.match(combinedSource, /주문 완료/);
  assert.match(orderListSource, /예약 내용 보기/);
  assert.match(orderListSource, /주문 내용 보기/);
  assert.match(source, /flowStep === "completed" && completedOrder && preview/);
  assert.match(source, /flowStep === "completedDetail" && completedDetail/);
  assert.match(completedDetailSource, /저장된 주문 내용을 확인해요/);
  assert.match(source, /handleViewCompletedOrder/);
  assert.match(source, /handleDeleteCompletedOrder/);
  assert.doesNotMatch(combinedSource, /handleDownloadFromOrder/);
  assert.match(combinedSource, /JSON 다운로드/);
  assert.match(combinedSource, /텍스트 다운로드/);
});

await runTest("export client keeps order history available when diary entries are empty", async () => {
  const source = await readFile(new URL("../src/app/export/export-client.tsx", import.meta.url), "utf8");

  assert.doesNotMatch(source, /if \(!entries\.length\)\s*\{\s*return <DiaryEmptyState \/>;\s*\}/);
  assert.match(source, /<ExportTabs activeTab=\{activeTab\} onTabChange=\{setActiveTab\} \/>/);
  assert.match(source, /!entries\.length \? \(\s*<DiaryEmptyState \/>/);
});

await runTest("export API client exposes order history and detail reads", async () => {
  const source = await readFile(new URL("../src/lib/api/export.ts", import.meta.url), "utf8");
  const routeSource = await readFile(new URL("../src/app/api/exports/route.ts", import.meta.url), "utf8");
  const detailRouteSource = await readFile(new URL("../src/app/api/exports/[exportRequestId]/route.ts", import.meta.url), "utf8");

  assert.match(source, /getExportOrders/);
  assert.match(source, /getExportOrderDetail/);
  assert.match(source, /deleteExportOrder/);
  assert.match(routeSource, /export function GET/);
  assert.match(detailRouteSource, /export async function GET/);
  assert.match(detailRouteSource, /export async function DELETE/);
});

await runTest("order-facing page copy does not use placeholder text", async () => {
  const pageSource = await readFile(new URL("../src/app/export/page.tsx", import.meta.url), "utf8");
  assert.match(pageSource, /title="주문"/);
  assert.match(pageSource, /주문 미리보기/);
  assert.doesNotMatch(pageSource, /PlaceholderPanel/);
});

console.log("frontend export tests passed");
