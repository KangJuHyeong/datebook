import type { ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import type { StatusTone } from "@/types/app";
import type { ExportOrderSummary } from "@/types/api";

type OrderViewModel = {
  badge: string;
  tone: StatusTone;
  description: string;
  dateLabel: string;
};

type ExportOrderListProps = {
  orders: ExportOrderSummary[];
  ordersLoading: boolean;
  submitting: boolean;
  deletingOrderId: number | null;
  onRefresh: () => void;
  onResumeOrder: (order: ExportOrderSummary) => void;
  onViewCompletedOrder: (order: ExportOrderSummary) => void;
  onDeleteCompletedOrder: (orderId: number) => void;
  getOrderViewModel: (order: ExportOrderSummary) => OrderViewModel;
  skeleton: ReactNode;
};

export function ExportOrderList({
  orders,
  ordersLoading,
  submitting,
  deletingOrderId,
  onRefresh,
  onResumeOrder,
  onViewCompletedOrder,
  onDeleteCompletedOrder,
  getOrderViewModel,
  skeleton,
}: ExportOrderListProps) {
  return (
    <>
      <section className="rounded-lg border border-stone-200 bg-white p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="space-y-2">
            <StatusBadge tone="progress">주문 내역</StatusBadge>
            <h2 className="text-base font-semibold text-stone-950">진행 중이거나 완료된 주문을 확인해요.</h2>
            <p className="text-sm leading-6 text-stone-700">
              주문 예약 단계의 주문은 이어서 완료할 수 있고, 완료된 주문은 저장된 내용을 확인하거나 삭제할 수 있어요.
            </p>
          </div>
          <Button variant="secondary" onClick={onRefresh} disabled={ordersLoading}>
            {ordersLoading ? "불러오는 중..." : "새로고침"}
          </Button>
        </div>
      </section>

      {ordersLoading ? skeleton : null}

      {!ordersLoading && !orders.length ? (
        <section className="rounded-lg border border-stone-200 bg-white p-6">
          <div className="space-y-3">
            <StatusBadge tone="waiting">주문 없음</StatusBadge>
            <h2 className="text-sm font-semibold text-stone-900">아직 이어갈 주문이 없어요.</h2>
            <p className="text-sm leading-6 text-stone-700">기록을 선택해 주문 신청을 시작해보세요.</p>
          </div>
        </section>
      ) : null}

      {!ordersLoading && orders.length ? (
        <section className="space-y-4" aria-label="주문 내역 목록">
          {orders.map((order) => {
            const viewModel = getOrderViewModel(order);
            return (
              <article key={order.exportRequestId} className="rounded-lg border border-stone-200 bg-white p-5">
                <div className="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
                  <div className="space-y-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <StatusBadge tone={viewModel.tone}>{viewModel.badge}</StatusBadge>
                      <span className="text-xs text-stone-500">주문 #{order.exportRequestId}</span>
                    </div>
                    <div className="space-y-1">
                      <h3 className="text-base font-semibold text-stone-950">선택한 기록 {order.itemCount}개</h3>
                      <p className="text-sm leading-6 text-stone-700">{viewModel.description}</p>
                      <p className="text-xs text-stone-500">{viewModel.dateLabel}</p>
                    </div>
                  </div>
                  <div className="flex flex-col gap-2 sm:flex-row md:flex-col">
                    {order.status === "PREVIEWED" ? (
                      <Button onClick={() => onResumeOrder(order)} disabled={submitting}>
                        예약 내용 보기
                      </Button>
                    ) : null}
                    {order.status === "COMPLETED" ? (
                      <>
                        <Button onClick={() => onViewCompletedOrder(order)} disabled={submitting}>
                          주문 내용 보기
                        </Button>
                        <Button
                          variant="danger"
                          onClick={() => onDeleteCompletedOrder(order.exportRequestId)}
                          disabled={deletingOrderId !== null}
                        >
                          {deletingOrderId === order.exportRequestId ? "삭제 중..." : "삭제"}
                        </Button>
                      </>
                    ) : null}
                  </div>
                </div>
              </article>
            );
          })}
        </section>
      ) : null}
    </>
  );
}
