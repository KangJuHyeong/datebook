import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import type { ExportOrderDetailResponse } from "@/types/api";

type DownloadFormat = "json" | "text";

type ExportCompletedDetailProps = {
  detail: ExportOrderDetailResponse;
  deletingOrderId: number | null;
  downloadingFormat: DownloadFormat | null;
  onDownload: (format: DownloadFormat) => void;
  onDelete: (orderId: number) => void;
  onBack: () => void;
  formatDateTime: (value: string | null) => string | null;
};

export function ExportCompletedDetail({
  detail,
  deletingOrderId,
  downloadingFormat,
  onDownload,
  onDelete,
  onBack,
  formatDateTime,
}: ExportCompletedDetailProps) {
  return (
    <>
      <section className="rounded-lg border border-stone-200 bg-white p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="space-y-3">
            <StatusBadge tone="success">주문 완료</StatusBadge>
            <h2 className="text-base font-semibold text-stone-950">저장된 주문 내용을 확인해요.</h2>
            <p className="text-sm leading-6 text-stone-700">
              완료된 주문은 저장된 스냅샷으로 보여줘요. 내용을 바꾸려면 새 주문을 만들어주세요.
            </p>
            <p className="text-xs text-stone-500">완료 시각 {formatDateTime(detail.completedAt) ?? detail.completedAt}</p>
          </div>
          <StatusBadge tone="neutral">선택한 기록 {detail.entries?.length ?? detail.itemCount}개</StatusBadge>
        </div>
        <div className="mt-4 flex flex-col gap-3 sm:flex-row">
          <Button onClick={() => onDownload("json")} disabled={downloadingFormat !== null} data-testid="download-json">
            {downloadingFormat === "json" ? "JSON 다운로드 중..." : "JSON 다운로드"}
          </Button>
          <Button
            variant="secondary"
            onClick={() => onDownload("text")}
            disabled={downloadingFormat !== null}
            data-testid="download-text"
          >
            {downloadingFormat === "text" ? "텍스트 다운로드 중..." : "텍스트 다운로드"}
          </Button>
          <Button variant="danger" onClick={() => onDelete(detail.exportRequestId)} disabled={deletingOrderId !== null}>
            {deletingOrderId === detail.exportRequestId ? "삭제 중..." : "삭제"}
          </Button>
          <Button variant="text" onClick={onBack}>
            주문 내역으로 돌아가기
          </Button>
        </div>
      </section>

      <section className="space-y-4" aria-label="주문 완료 저장 스냅샷">
        {detail.entries?.map((entry, index) => (
          <article key={`${entry.date}-${index}`} className="rounded-lg border border-stone-200 bg-white p-5">
            <div className="space-y-2">
              <p className="text-xs text-stone-500">{entry.date}</p>
              <h3 className="text-base font-medium leading-7 text-stone-950">{entry.question}</h3>
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
  );
}
