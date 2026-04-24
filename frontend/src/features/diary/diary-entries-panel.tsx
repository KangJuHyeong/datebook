"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { StatusBadge } from "@/components/ui/status-badge";
import { getDiaryEntries } from "@/lib/api/diary";
import type { DiaryEntry } from "@/types/api";
import { getDiaryEntryViewModel } from "./diary-view-logic.mjs";

function DiarySkeleton() {
  return (
    <section className="space-y-4" aria-label="기록 로딩">
      {Array.from({ length: 3 }).map((_, index) => (
        <div key={index} className="min-h-44 animate-pulse rounded-lg border border-stone-200 bg-white p-5">
          <div className="h-4 w-20 rounded bg-stone-200" />
          <div className="mt-4 h-6 w-3/4 rounded bg-stone-200" />
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
        <p className="text-sm leading-6 text-stone-700">오늘 질문부터 시작해보세요.</p>
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

export function DiaryEntriesPanel() {
  const [entries, setEntries] = useState<DiaryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    let active = true;

    async function loadDiaryEntries() {
      setLoading(true);
      setError(undefined);

      try {
        const response = await getDiaryEntries();

        if (!active) {
          return;
        }

        setEntries(response.entries);
      } catch (caught) {
        if (!active) {
          return;
        }

        setError(caught instanceof Error ? caught.message : "잠시 후 다시 시도해주세요.");
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadDiaryEntries();

    return () => {
      active = false;
    };
  }, []);

  const entryViewModels = useMemo(
    () => entries.map((entry) => ({ entry, viewModel: getDiaryEntryViewModel(entry) })),
    [entries],
  );

  if (loading) {
    return <DiarySkeleton />;
  }

  if (error) {
    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-5" role="alert">
        <p className="text-sm font-medium text-red-700">{error}</p>
      </section>
    );
  }

  if (!entries.length) {
    return <DiaryEmptyState />;
  }

  return (
    <section className="space-y-4">
      {entryViewModels.map(({ entry, viewModel }) => (
        <article key={entry.dailyQuestionId} className="rounded-lg border border-stone-200 bg-white p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div className="space-y-2">
              <p className="text-xs text-stone-500">{viewModel.dateLabel}</p>
              <h2 className="text-base font-medium leading-7 text-stone-950">{entry.question}</h2>
            </div>
            <StatusBadge tone={entry.exportable ? "selected" : "waiting"}>
              {entry.exportable ? "주문 가능" : "잠금 포함"}
            </StatusBadge>
          </div>
          <div className="mt-4 grid gap-3 md:grid-cols-2">
            <div className="rounded-lg border border-stone-200 bg-stone-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-sm font-semibold text-stone-900">내 답변 상태</h3>
                <StatusBadge tone={viewModel.myAnswerTone}>{viewModel.myAnswerBadge}</StatusBadge>
              </div>
              <p className="mt-3 text-sm leading-6 text-stone-700">
                {entry.myAnswerStatus === "ANSWERED" ? "내 답변은 기록에 남아 있어요." : "아직 내 답변을 남기지 않았어요."}
              </p>
            </div>
            <div className="rounded-lg border border-stone-200 bg-stone-50 p-4">
              <div className="flex items-center justify-between gap-3">
                <h3 className="text-sm font-semibold text-stone-900">상대 답변 상태</h3>
                <StatusBadge tone={viewModel.partnerAnswerTone}>{viewModel.partnerAnswerBadge}</StatusBadge>
              </div>
              <p className="mt-3 text-sm leading-6 text-stone-700">{viewModel.partnerStatusCopy}</p>
            </div>
          </div>
          <p className="mt-4 text-sm text-stone-500">{viewModel.exportableCopy}</p>
        </article>
      ))}
    </section>
  );
}
