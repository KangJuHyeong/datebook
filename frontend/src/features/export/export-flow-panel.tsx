"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { getDiaryEntries } from "@/lib/api/diary";
import {
  cancelExportOrder,
  completeExportOrder,
  createExportOrder,
  downloadExportOrder,
  previewExportOrder,
} from "@/lib/api/export";
import type { CompleteExportResponse, DiaryEntry, ExportPreviewResponse } from "@/types/api";
import {
  cancelExportSelection,
  getExportEntryViewModel,
  submitExportCompletion,
  submitExportSelection,
  toggleExportSelection,
} from "./export-flow-logic.mjs";

type FlowStep = "select" | "preview" | "completed";
type DownloadFormat = "json" | "text";

function DiarySelectionSkeleton() {
  return (
    <section className="space-y-4" aria-label="주문 기록 로딩">
      {Array.from({ length: 3 }).map((_, index) => (
        <div key={index} className="min-h-40 animate-pulse rounded-lg border border-stone-200 bg-white p-5">
          <div className="h-4 w-24 rounded bg-stone-200" />
          <div className="mt-4 h-6 w-4/5 rounded bg-stone-200" />
          <div className="mt-4 h-4 w-full rounded bg-stone-100" />
          <div className="mt-2 h-4 w-2/3 rounded bg-stone-100" />
        </div>
      ))}
    </section>
  );
}

function DiaryEmptyState() {
  return (
    <section className="rounded-lg border border-stone-200 bg-white p-6">
      <div className="space-y-3">
        <StatusBadge tone="waiting">기록 없음</StatusBadge>
        <h2 className="text-sm font-semibold text-stone-900">아직 쌓인 기록이 없어요.</h2>
        <p className="text-sm leading-6 text-stone-700">오늘 질문부터 시작하면 주문할 기록도 함께 쌓여요.</p>
        <Link
          href="/today"
          className="inline-flex min-h-11 items-center rounded-md bg-rose-700 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-rose-800"
        >
          오늘 질문으로 가기
        </Link>
      </div>
    </section>
  );
}

function getDownloadFilename(response: Response, fallback: string) {
  const contentDisposition = response.headers.get("Content-Disposition");
  const matchedFilename = contentDisposition?.match(/filename="?([^"]+)"?/i)?.[1];
  return matchedFilename ?? fallback;
}

async function triggerDownload(exportRequestId: number, format: DownloadFormat) {
  const response = await downloadExportOrder(exportRequestId, format);

  if (!response.ok) {
    throw new Error("잠시 후 다시 시도해주세요.");
  }

  const blob = await response.blob();
  const objectUrl = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = getDownloadFilename(response, `couple-diary-${exportRequestId}.${format === "json" ? "json" : "txt"}`);
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(objectUrl);
}

export function ExportFlowPanel() {
  const [entries, setEntries] = useState<DiaryEntry[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();
  const [flowStep, setFlowStep] = useState<FlowStep>("select");
  const [submitting, setSubmitting] = useState(false);
  const [downloadingFormat, setDownloadingFormat] = useState<DownloadFormat | null>(null);
  const [exportRequestId, setExportRequestId] = useState<number | null>(null);
  const [preview, setPreview] = useState<ExportPreviewResponse | null>(null);
  const [completedOrder, setCompletedOrder] = useState<CompleteExportResponse | null>(null);

  async function loadEntries() {
    setLoading(true);
    setError(undefined);

    try {
      const response = await getDiaryEntries();
      setEntries(response.entries);
      setSelectedIds((currentIds) =>
        currentIds.filter((id) => response.entries.some((entry) => entry.dailyQuestionId === id && entry.exportable)),
      );
    } catch (caught) {
      const nextError = caught instanceof Error ? caught.message : "잠시 후 다시 시도해주세요.";
      setError(nextError);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadEntries();
  }, []);

  const exportableEntries = useMemo(() => entries.filter((entry) => entry.exportable), [entries]);
  const selectedCount = selectedIds.length;

  async function handleCreateOrder() {
    if (!selectedCount || submitting) {
      return;
    }

    setSubmitting(true);
    setError(undefined);

    const result = await submitExportSelection(selectedIds, {
      createExportOrder,
      previewExportOrder,
    });

    if (!result.ok) {
      if (result.redirectTo) {
        window.location.href = result.redirectTo;
        return;
      }

      setError(result.message);

      if (result.shouldRefetch) {
        await loadEntries();
      }

      setSubmitting(false);
      return;
    }

    setExportRequestId(result.exportRequestId);
    setPreview(result.preview);
    setCompletedOrder(null);
    setFlowStep("preview");
    setSubmitting(false);
  }

  async function handleCancel(mode: "reset" | "preserve") {
    if (!exportRequestId || submitting) {
      return;
    }

    setSubmitting(true);
    setError(undefined);

    const result = await cancelExportSelection(exportRequestId, {
      cancelExportOrder,
    });

    if (!result.ok) {
      if (result.redirectTo) {
        window.location.href = result.redirectTo;
        return;
      }

      setError(result.message);

      if (result.shouldRefetch) {
        await loadEntries();
      }

      setSubmitting(false);
      return;
    }

    setExportRequestId(null);
    setPreview(null);
    setCompletedOrder(null);
    setFlowStep("select");
    if (mode === "reset") {
      setSelectedIds([]);
    }
    setSubmitting(false);
  }

  async function handleCompleteOrder() {
    if (!exportRequestId || submitting) {
      return;
    }

    setSubmitting(true);
    setError(undefined);

    const result = await submitExportCompletion(exportRequestId, {
      completeExportOrder,
    });

    if (!result.ok) {
      if (result.redirectTo) {
        window.location.href = result.redirectTo;
        return;
      }

      setError(result.message);

      if (result.shouldRefetch) {
        try {
          const nextPreview = await previewExportOrder(exportRequestId);
          setPreview(nextPreview);
        } catch {
          setError("잠시 후 다시 시도해주세요.");
        }
      }

      setSubmitting(false);
      return;
    }

    setCompletedOrder(result.completed);
    setFlowStep("completed");
    setSubmitting(false);
  }

  async function handleDownload(format: DownloadFormat) {
    if (!exportRequestId || downloadingFormat) {
      return;
    }

    setDownloadingFormat(format);
    setError(undefined);

    try {
      await triggerDownload(exportRequestId, format);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "잠시 후 다시 시도해주세요.");
    } finally {
      setDownloadingFormat(null);
    }
  }

  if (loading) {
    return <DiarySelectionSkeleton />;
  }

  if (!entries.length) {
    return <DiaryEmptyState />;
  }

  return (
    <section className="space-y-6">
      {error ? (
        <section className="rounded-lg border border-red-200 bg-red-50 p-5" role="alert">
          <p className="text-sm font-medium text-red-700">{error}</p>
        </section>
      ) : null}

      {flowStep === "select" ? (
        <>
          <section className="rounded-lg border border-stone-200 bg-white p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="space-y-2">
                <StatusBadge tone="progress">기록 선택</StatusBadge>
                <h2 className="text-base font-semibold text-stone-950">주문할 기록을 골라주세요.</h2>
                <p className="text-sm leading-6 text-stone-700">
                  두 사람의 답변이 모두 열린 기록만 주문 미리보기와 다운로드에 담을 수 있어요.
                </p>
              </div>
              <div className="rounded-lg border border-stone-200 bg-stone-50 px-4 py-3 text-sm text-stone-700">
                <p className="font-medium text-stone-900">선택한 기록 {selectedCount}개</p>
                <p className="mt-1 text-xs text-stone-500">JSON 다운로드와 텍스트 다운로드는 주문 완료 뒤에 열려요.</p>
              </div>
            </div>
          </section>

          <section className="space-y-4" aria-label="주문 기록 목록">
            {entries.map((entry) => {
              const checked = selectedIds.includes(entry.dailyQuestionId);
              const viewModel = getExportEntryViewModel(entry);

              return (
                <article key={entry.dailyQuestionId} className="rounded-lg border border-stone-200 bg-white p-5">
                  <div
                    role="checkbox"
                    aria-checked={checked}
                    aria-disabled={!entry.exportable}
                    tabIndex={entry.exportable ? 0 : -1}
                    className={`cursor-pointer outline-none transition-colors focus:ring-2 focus:ring-rose-100 ${
                      entry.exportable ? "" : "cursor-not-allowed opacity-80"
                    }`}
                    onClick={() => {
                      setSelectedIds((currentIds) =>
                        toggleExportSelection(currentIds, entry.dailyQuestionId, entry.exportable),
                      );
                    }}
                    onKeyDown={(event) => {
                      if (event.key === " " || event.key === "Spacebar") {
                        event.preventDefault();
                        setSelectedIds((currentIds) =>
                          toggleExportSelection(currentIds, entry.dailyQuestionId, entry.exportable),
                        );
                      }
                    }}
                  >
                    <div className="flex min-h-11 items-start gap-4">
                      <input
                        type="checkbox"
                        checked={checked}
                        readOnly
                        tabIndex={-1}
                        aria-hidden="true"
                        className="mt-1 h-4 w-4 rounded border-stone-300 text-rose-700 focus:ring-rose-500"
                        disabled={!entry.exportable}
                      />
                      <div className="min-w-0 flex-1 space-y-4">
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div className="space-y-2">
                            <p className="text-xs text-stone-500">{viewModel.dateLabel}</p>
                            <h3 className="text-base font-medium leading-7 text-stone-950">{entry.question}</h3>
                          </div>
                          <StatusBadge tone={viewModel.availabilityTone}>{viewModel.availabilityBadge}</StatusBadge>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <StatusBadge tone={viewModel.myAnswerTone}>{viewModel.myAnswerBadge}</StatusBadge>
                          <StatusBadge tone={viewModel.partnerAnswerTone}>{viewModel.partnerAnswerBadge}</StatusBadge>
                          {checked ? <StatusBadge tone="selected">선택됨</StatusBadge> : null}
                        </div>
                        <p className="text-sm leading-6 text-stone-700">{viewModel.availabilityDescription}</p>
                      </div>
                    </div>
                  </div>
                </article>
              );
            })}
          </section>

          <div className="sticky bottom-4 z-10">
            <section className="rounded-lg border border-stone-200 bg-white p-4 shadow-sm">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="space-y-1">
                  <p className="text-sm font-semibold text-stone-900">선택한 기록 {selectedCount}개</p>
                  <p className="text-sm text-stone-500">
                    {exportableEntries.length
                      ? "둘 다 답한 기록만 주문할 수 있어요."
                      : "둘 다 답한 기록만 주문할 수 있어요."}
                  </p>
                </div>
                <Button onClick={handleCreateOrder} disabled={!selectedCount || !exportableEntries.length || submitting}>
                  {submitting ? "주문 준비 중..." : "주문 신청"}
                </Button>
              </div>
            </section>
          </div>
        </>
      ) : null}

      {flowStep === "preview" && preview ? (
        <>
          <section className="rounded-lg border border-stone-200 bg-white p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div className="space-y-2">
                <StatusBadge tone="progress">주문 미리보기</StatusBadge>
                <h2 className="text-base font-semibold text-stone-950">다운로드에 담길 기록을 마지막으로 확인해주세요.</h2>
                <p className="text-sm leading-6 text-stone-700">
                  미리보기에는 실제 다운로드에 포함될 내용만 보여줘요.
                </p>
              </div>
              <p className="text-sm text-stone-500">선택한 기록 {preview.entries.length}개</p>
            </div>
          </section>

          <section className="space-y-4" aria-label="주문 미리보기 목록">
            {preview.entries.map((entry, index) => (
              <article key={`${entry.date}-${index}`} className="rounded-lg border border-stone-200 bg-white p-5">
                <div className="space-y-2">
                  <p className="text-xs text-stone-500">{entry.date}</p>
                  <h3 className="text-base font-medium leading-7 text-stone-950">{entry.question}</h3>
                </div>
                <div className="mt-4 space-y-4">
                  {entry.answers.map((answer) => (
                    <div key={answer.displayName} className="border-t border-stone-100 pt-4 first:border-t-0 first:pt-0">
                      <p className="text-sm font-semibold text-stone-900">{answer.displayName}</p>
                      <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-stone-700">{answer.content}</p>
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </section>

          <section className="sticky bottom-4 z-10 rounded-lg border border-stone-200 bg-white p-4 shadow-sm">
            <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
              <p className="text-sm text-stone-700">주문을 완료하면 JSON 다운로드와 텍스트 다운로드 버튼이 열려요.</p>
              <div className="flex flex-col gap-3 sm:flex-row">
                <Button variant="secondary" onClick={() => void handleCancel("preserve")} disabled={submitting}>
                  다시 선택
                </Button>
                <Button variant="text" onClick={() => void handleCancel("reset")} disabled={submitting}>
                  취소
                </Button>
                <Button onClick={handleCompleteOrder} disabled={submitting}>
                  {submitting ? "주문 완료 중..." : "주문 완료"}
                </Button>
              </div>
            </div>
          </section>
        </>
      ) : null}

      {flowStep === "completed" && completedOrder && preview ? (
        <>
          <section className="rounded-lg border border-stone-200 bg-white p-5">
            <div className="space-y-3">
              <StatusBadge tone="success">주문 완료</StatusBadge>
              <h2 className="text-base font-semibold text-stone-950">주문이 완료됐어요.</h2>
              <p className="text-sm leading-6 text-stone-700">
                선택한 기록 {preview.entries.length}개를 같은 내용으로 다시 내려받을 수 있어요.
              </p>
            </div>
            <div className="mt-4 flex flex-col gap-3 sm:flex-row">
              <Button onClick={() => void handleDownload("json")} disabled={downloadingFormat !== null}>
                {downloadingFormat === "json" ? "JSON 다운로드 중..." : "JSON 다운로드"}
              </Button>
              <Button variant="secondary" onClick={() => void handleDownload("text")} disabled={downloadingFormat !== null}>
                {downloadingFormat === "text" ? "텍스트 다운로드 중..." : "텍스트 다운로드"}
              </Button>
            </div>
            <p className="mt-4 text-xs text-stone-500">완료 시각 {completedOrder.completedAt}</p>
          </section>

          <section className="space-y-4" aria-label="주문 완료 기록 요약">
            {preview.entries.map((entry, index) => (
              <article key={`${entry.date}-${index}`} className="rounded-lg border border-stone-200 bg-white p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-2">
                    <p className="text-xs text-stone-500">{entry.date}</p>
                    <h3 className="text-base font-medium leading-7 text-stone-950">{entry.question}</h3>
                  </div>
                  <StatusBadge tone="success">다운로드 가능</StatusBadge>
                </div>
                <ul className="mt-4 space-y-4">
                  {entry.answers.map((answer) => (
                    <li key={answer.displayName} className="border-t border-stone-100 pt-4 first:border-t-0 first:pt-0">
                      <p className="text-sm font-semibold text-stone-900">{answer.displayName}</p>
                      <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-stone-700">{answer.content}</p>
                    </li>
                  ))}
                </ul>
              </article>
            ))}
          </section>
        </>
      ) : null}
    </section>
  );
}
