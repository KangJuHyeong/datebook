import type { Dispatch, SetStateAction } from "react";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { getExportEntryViewModel, toggleExportSelection } from "@/features/export/export-flow-logic.mjs";
import type { StatusTone } from "@/types/app";
import type { DiaryEntry, ExportPreviewAnswer } from "@/types/api";

type ExportSelectionPanelProps = {
  entries: DiaryEntry[];
  exportableEntries: DiaryEntry[];
  selectedIds: number[];
  selectedCount: number;
  submitting: boolean;
  onCreateOrder: () => void;
  setSelectedIds: Dispatch<SetStateAction<number[]>>;
};

export function ExportSelectionPanel({
  entries,
  exportableEntries,
  selectedIds,
  selectedCount,
  submitting,
  onCreateOrder,
  setSelectedIds,
}: ExportSelectionPanelProps) {
  return (
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
                data-testid={`export-entry-${entry.dailyQuestionId}`}
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
                      <StatusBadge tone={viewModel.availabilityTone as StatusTone}>
                        {viewModel.availabilityBadge}
                      </StatusBadge>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <StatusBadge tone={viewModel.myAnswerTone as StatusTone}>{viewModel.myAnswerBadge}</StatusBadge>
                      <StatusBadge tone={viewModel.partnerAnswerTone as StatusTone}>
                        {viewModel.partnerAnswerBadge}
                      </StatusBadge>
                      {checked ? <StatusBadge tone="selected">선택됨</StatusBadge> : null}
                    </div>
                    <p className="text-sm leading-6 text-stone-700">{viewModel.availabilityDescription}</p>
                    {entry.exportable ? (
                      <div className="grid gap-3 md:grid-cols-2">
                        {[entry.myAnswer, entry.partnerAnswer]
                          .filter((answer): answer is ExportPreviewAnswer => Boolean(answer))
                          .map((answer) => (
                            <div key={answer.displayName} className="rounded-lg border border-stone-200 bg-stone-50 p-4">
                              <p className="text-xs font-medium text-stone-500">{answer.displayName}</p>
                              <p className="mt-2 line-clamp-4 whitespace-pre-wrap text-sm leading-6 text-stone-700">
                                {answer.content}
                              </p>
                            </div>
                          ))}
                      </div>
                    ) : null}
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
              <p className="text-sm text-stone-500">둘 다 답한 기록만 주문할 수 있어요.</p>
            </div>
            <Button
              onClick={onCreateOrder}
              disabled={!selectedCount || !exportableEntries.length || submitting}
              data-testid="create-export-order"
            >
              {submitting ? "주문 준비 중..." : "주문 신청"}
            </Button>
          </div>
        </section>
      </div>
    </>
  );
}
