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
  deleteExportOrder,
  downloadExportOrder,
  getExportOrderDetail,
  getExportOrders,
  previewExportOrder,
} from "@/lib/api/export";
import type {
  CompleteExportResponse,
  DiaryEntry,
  ExportOrderDetailResponse,
  ExportOrderSummary,
  ExportPreviewResponse,
} from "@/types/api";
import {
  cancelExportSelection,
  submitExportCompletion,
  submitExportSelection,
} from "@/features/export/export-flow-logic.mjs";
import { ExportCompletedDetail } from "./export-completed-detail";
import { ExportOrderList } from "./export-order-list";
import { ExportSelectionPanel } from "./export-selection-panel";
import { ExportTabs } from "./export-tabs";

type FlowStep = "select" | "preview" | "completed" | "completedDetail";
type DownloadFormat = "json" | "text";
type ExportTab = "select" | "orders";

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

function formatDateTime(value: string | null) {
  if (!value) {
    return null;
  }

  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function getOrderViewModel(order: ExportOrderSummary) {
  const isPreviewed = order.status === "PREVIEWED";
  const isCompleted = order.status === "COMPLETED";

  return {
    badge: isPreviewed ? "주문 예약 단계" : isCompleted ? "주문 완료" : order.status === "CANCELLED" ? "취소됨" : "주문 신청",
    tone: isCompleted ? "success" : isPreviewed ? "progress" : order.status === "CANCELLED" ? "waiting" : "neutral",
    description: isPreviewed
      ? "예약 내용을 다시 열어 주문 완료 또는 취소를 이어갈 수 있어요."
      : isCompleted
        ? "완료된 주문은 저장된 스냅샷을 확인하거나 삭제할 수 있어요."
        : "현재 상태를 확인해주세요.",
    dateLabel: formatDateTime(order.completedAt ?? order.previewedAt ?? order.createdAt) ?? "",
  } as const;
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

export function ExportClient() {
  const [activeTab, setActiveTab] = useState<ExportTab>("select");
  const [entries, setEntries] = useState<DiaryEntry[]>([]);
  const [orders, setOrders] = useState<ExportOrderSummary[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [loading, setLoading] = useState(true);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [error, setError] = useState<string>();
  const [flowStep, setFlowStep] = useState<FlowStep>("select");
  const [submitting, setSubmitting] = useState(false);
  const [downloadingFormat, setDownloadingFormat] = useState<DownloadFormat | null>(null);
  const [deletingOrderId, setDeletingOrderId] = useState<number | null>(null);
  const [exportRequestId, setExportRequestId] = useState<number | null>(null);
  const [preview, setPreview] = useState<ExportPreviewResponse | null>(null);
  const [completedOrder, setCompletedOrder] = useState<CompleteExportResponse | null>(null);
  const [completedDetail, setCompletedDetail] = useState<ExportOrderDetailResponse | null>(null);

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

  async function loadOrders() {
    setOrdersLoading(true);
    setError(undefined);

    try {
      const response = await getExportOrders();
      setOrders(response.orders);
    } catch (caught) {
      const nextError = caught instanceof Error ? caught.message : "잠시 후 다시 시도해주세요.";
      setError(nextError);
    } finally {
      setOrdersLoading(false);
    }
  }

  useEffect(() => {
    if (activeTab === "orders" && flowStep === "select") {
      void loadOrders();
    }
  }, [activeTab, flowStep]);

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
    setCompletedDetail(null);
    setFlowStep("preview");
    setActiveTab("select");
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
    setCompletedDetail(null);
    setFlowStep("select");
    if (mode === "reset") {
      setSelectedIds([]);
    }
    await loadOrders();
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
    setCompletedDetail(null);
    setFlowStep("completed");
    await loadOrders();
    setSubmitting(false);
  }

  async function handleResumeOrder(order: ExportOrderSummary) {
    if (submitting) {
      return;
    }

    setSubmitting(true);
    setError(undefined);

    try {
      const detail = await getExportOrderDetail(order.exportRequestId);
      if (detail.status !== "PREVIEWED" || !detail.entries) {
        setError("이어갈 수 있는 미리보기 주문이 아니에요.");
        setSubmitting(false);
        return;
      }

      setExportRequestId(detail.exportRequestId);
      setPreview({
        exportRequestId: detail.exportRequestId,
        status: detail.status,
        entries: detail.entries,
      });
      setCompletedOrder(null);
      setCompletedDetail(null);
      setFlowStep("preview");
      setActiveTab("select");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleViewCompletedOrder(order: ExportOrderSummary) {
    if (submitting) {
      return;
    }

    setSubmitting(true);
    setError(undefined);

    try {
      const detail = await getExportOrderDetail(order.exportRequestId);
      if (detail.status !== "COMPLETED" || !detail.entries) {
        setError("확인할 수 있는 주문 완료 내역이 아니에요.");
        setSubmitting(false);
        return;
      }

      setExportRequestId(detail.exportRequestId);
      setCompletedDetail(detail);
      setPreview(null);
      setCompletedOrder(null);
      setFlowStep("completedDetail");
      setActiveTab("orders");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteCompletedOrder(orderId: number) {
    if (deletingOrderId) {
      return;
    }

    const confirmed = window.confirm("완료된 주문을 삭제할까요? 삭제한 주문은 다시 다운로드할 수 없어요.");
    if (!confirmed) {
      return;
    }

    setDeletingOrderId(orderId);
    setError(undefined);

    try {
      await deleteExportOrder(orderId);
      if (completedDetail?.exportRequestId === orderId) {
        setCompletedDetail(null);
        setExportRequestId(null);
        setFlowStep("select");
        setActiveTab("orders");
      }
      await loadOrders();
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "잠시 후 다시 시도해주세요.");
    } finally {
      setDeletingOrderId(null);
    }
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

  return (
    <section className="space-y-6">
      {error ? (
        <section className="rounded-lg border border-red-200 bg-red-50 p-5" role="alert">
          <p className="text-sm font-medium text-red-700">{error}</p>
        </section>
      ) : null}

      {flowStep === "select" ? (
        <>
          <ExportTabs activeTab={activeTab} onTabChange={setActiveTab} />

          {activeTab === "orders" ? (
            <ExportOrderList
              orders={orders}
              ordersLoading={ordersLoading}
              submitting={submitting}
              deletingOrderId={deletingOrderId}
              onRefresh={() => void loadOrders()}
              onResumeOrder={(order) => void handleResumeOrder(order)}
              onViewCompletedOrder={(order) => void handleViewCompletedOrder(order)}
              onDeleteCompletedOrder={(orderId) => void handleDeleteCompletedOrder(orderId)}
              getOrderViewModel={getOrderViewModel}
              skeleton={<DiarySelectionSkeleton />}
            />
          ) : loading ? (
            <DiarySelectionSkeleton />
          ) : !entries.length ? (
            <DiaryEmptyState />
          ) : (
            <ExportSelectionPanel
              entries={entries}
              exportableEntries={exportableEntries}
              selectedIds={selectedIds}
              selectedCount={selectedCount}
              submitting={submitting}
              onCreateOrder={handleCreateOrder}
              setSelectedIds={setSelectedIds}
            />
          )}
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
                <Button onClick={handleCompleteOrder} disabled={submitting} data-testid="complete-export-order">
                  {submitting ? "주문 완료 중..." : "주문 완료"}
                </Button>
              </div>
            </div>
          </section>
        </>
      ) : null}

      {flowStep === "completedDetail" && completedDetail ? (
        <ExportCompletedDetail
          detail={completedDetail}
          deletingOrderId={deletingOrderId}
          downloadingFormat={downloadingFormat}
          onDownload={(format) => void handleDownload(format)}
          onDelete={(orderId) => void handleDeleteCompletedOrder(orderId)}
          onBack={() => {
            setCompletedDetail(null);
            setExportRequestId(null);
            setFlowStep("select");
            setActiveTab("orders");
          }}
          formatDateTime={formatDateTime}
        />
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
              <Button
                onClick={() => void handleDownload("json")}
                disabled={downloadingFormat !== null}
                data-testid="download-json"
              >
                {downloadingFormat === "json" ? "JSON 다운로드 중..." : "JSON 다운로드"}
              </Button>
              <Button
                variant="secondary"
                onClick={() => void handleDownload("text")}
                disabled={downloadingFormat !== null}
                data-testid="download-text"
              >
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
                  <StatusBadge tone="success">주문 완료</StatusBadge>
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
