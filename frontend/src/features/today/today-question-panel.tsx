"use client";

import { useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { StatusBadge } from "@/components/ui/status-badge";
import { createAnswer, getTodayQuestion, updateAnswer } from "@/lib/api/today";
import type { TodayQuestionResponse } from "@/types/api";
import { ANSWER_MAX_LENGTH, getTodayViewModel, submitTodayAnswer, validateAnswerContent } from "./today-form-logic.mjs";

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function TodayQuestionSkeleton() {
  return (
    <section className="space-y-4" aria-label="오늘 질문 로딩">
      <div className="min-h-40 animate-pulse rounded-lg border border-stone-200 bg-white p-5">
        <div className="h-4 w-24 rounded bg-stone-200" />
        <div className="mt-4 h-8 w-4/5 rounded bg-stone-200" />
        <div className="mt-3 h-4 w-2/3 rounded bg-stone-100" />
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="min-h-72 animate-pulse rounded-lg border border-stone-200 bg-white p-5">
          <div className="h-4 w-20 rounded bg-stone-200" />
          <div className="mt-4 h-32 rounded bg-stone-100" />
          <div className="mt-4 h-10 w-28 rounded bg-stone-200" />
        </div>
        <div className="min-h-72 animate-pulse rounded-lg border border-stone-200 bg-white p-5">
          <div className="h-4 w-20 rounded bg-stone-200" />
          <div className="mt-4 h-28 rounded bg-stone-100" />
        </div>
      </div>
    </section>
  );
}

export function TodayQuestionPanel() {
  const [todayQuestion, setTodayQuestion] = useState<TodayQuestionResponse | null>(null);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [fieldError, setFieldError] = useState<string>();
  const [formError, setFormError] = useState<string>();
  const [successMessage, setSuccessMessage] = useState<string>();

  useEffect(() => {
    let active = true;

    async function loadTodayQuestion() {
      setLoading(true);
      setFormError(undefined);

      try {
        const response = await getTodayQuestion();

        if (!active) {
          return;
        }

        setTodayQuestion(response);
        setDraft(response.myAnswer?.content ?? "");
      } catch (error) {
        if (!active) {
          return;
        }

        setFormError(error instanceof Error ? error.message : "잠시 후 다시 시도해주세요.");
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadTodayQuestion();

    return () => {
      active = false;
    };
  }, []);

  const validationError = useMemo(() => validateAnswerContent(draft), [draft]);
  const hasUnsavedChanges = Boolean(todayQuestion) && draft !== (todayQuestion?.myAnswer?.content ?? "");
  const viewModel = todayQuestion ? getTodayViewModel(todayQuestion) : null;

  useEffect(() => {
    if (!hasUnsavedChanges) {
      return undefined;
    }

    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [hasUnsavedChanges]);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!todayQuestion || saving || validationError) {
      setFieldError(validationError);
      return;
    }

    setSaving(true);
    setFieldError(undefined);
    setFormError(undefined);
    setSuccessMessage(undefined);

    const result = await submitTodayAnswer(
      { todayQuestion, content: draft },
      { createAnswer, updateAnswer, getTodayQuestion },
    );

    if (!result.ok) {
      setFieldError(result.fieldErrors?.content);
      setFormError(result.formError);
      if (result.redirectTo) {
        window.location.href = result.redirectTo;
        return;
      }
      if (result.shouldRefetch) {
        try {
          const response = await getTodayQuestion();
          setTodayQuestion(response);
          setDraft(response.myAnswer?.content ?? "");
        } catch {
          // Keep the current error state if refetch fails.
        }
      }
      setSaving(false);
      return;
    }

    setTodayQuestion(result.todayQuestion);
    setDraft(result.todayQuestion.myAnswer?.content ?? "");
    setSuccessMessage(result.successMessage);
    setSaving(false);
  }

  if (loading) {
    return <TodayQuestionSkeleton />;
  }

  if (!todayQuestion || !viewModel) {
    return (
      <section className="rounded-lg border border-red-200 bg-red-50 p-5" role="alert">
        <p className="text-sm font-medium text-red-700">{formError ?? "잠시 후 다시 시도해주세요."}</p>
      </section>
    );
  }

  return (
    <section className="space-y-4">
      <article className="rounded-lg border border-stone-200 bg-white p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-xs text-stone-500">{viewModel.questionDateLabel}</p>
          <StatusBadge tone={todayQuestion.isFullyAnswered ? "success" : "waiting"}>
            {todayQuestion.isFullyAnswered ? "두 사람 모두 답했어요" : "답변 진행 중"}
          </StatusBadge>
        </div>
        <p className="mt-4 text-xl font-medium leading-relaxed text-stone-950">{todayQuestion.question}</p>
        <p className="mt-3 text-sm text-stone-500">{viewModel.statusCopy}</p>
      </article>

      <div className="grid gap-4 md:grid-cols-2">
        <article className="rounded-lg border border-stone-200 bg-white p-5">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-sm font-semibold text-stone-900">내 답변</h2>
            <StatusBadge tone={viewModel.myAnswer.tone}>{viewModel.myAnswer.badge}</StatusBadge>
          </div>
          <p className="mt-3 text-sm leading-6 text-stone-700">{viewModel.myAnswer.description}</p>
          <form className="mt-4 space-y-3" onSubmit={handleSubmit} noValidate>
            <label className="block space-y-2">
              <span className="text-sm font-medium text-stone-900">답변</span>
              <textarea
                id="today-answer"
                name="content"
                value={draft}
                onChange={(event) => {
                  setDraft(event.target.value);
                  setFieldError(undefined);
                  setSuccessMessage(undefined);
                }}
                aria-invalid={Boolean(fieldError || validationError)}
                aria-describedby={fieldError || validationError ? "today-answer-error" : undefined}
                className="min-h-44 w-full rounded-md border border-stone-300 bg-white px-3 py-3 text-sm leading-6 text-stone-950 placeholder:text-stone-400 focus:border-rose-500 focus:outline-none focus:ring-2 focus:ring-rose-100"
                placeholder="오늘의 마음을 차분하게 적어보세요."
                maxLength={ANSWER_MAX_LENGTH}
              />
            </label>
            <div className="flex items-center justify-between gap-3 text-xs text-stone-500">
              <span>{todayQuestion.myAnswer?.updatedAt ? `마지막 저장 ${formatDateTime(todayQuestion.myAnswer.updatedAt)}` : "아직 저장된 답변이 없어요."}</span>
              <span aria-live="polite">{draft.length}/{ANSWER_MAX_LENGTH}</span>
            </div>
            {fieldError || validationError ? (
              <p id="today-answer-error" className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
                {fieldError ?? validationError}
              </p>
            ) : null}
            {formError ? (
              <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
                {formError}
              </p>
            ) : null}
            {successMessage ? (
              <p className="rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700" role="status">
                {successMessage}
              </p>
            ) : null}
            {hasUnsavedChanges ? (
              <p className="text-sm text-stone-500">저장하지 않은 변경이 있어요. 페이지를 나가면 브라우저가 한 번 더 확인해요.</p>
            ) : null}
            <Button type="submit" disabled={saving || Boolean(validationError)}>
              {saving ? "저장 중..." : todayQuestion.myAnswer ? "답변 수정하기" : "답변 저장하기"}
            </Button>
          </form>
        </article>

        <article className="rounded-lg border border-stone-200 bg-white p-5">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-sm font-semibold text-stone-900">상대 답변</h2>
            <StatusBadge tone={viewModel.partnerAnswer.tone}>{viewModel.partnerAnswer.badge}</StatusBadge>
          </div>
          <div
            className={`mt-4 rounded-lg border p-4 ${
              viewModel.partnerAnswer.content
                ? "border-emerald-200 bg-emerald-50"
                : "border-stone-200 bg-stone-100"
            }`}
            aria-live="polite"
          >
            <p className="text-sm leading-6 text-stone-700">{viewModel.partnerAnswer.description}</p>
            {viewModel.partnerAnswer.content ? (
              <>
                <p className="mt-4 whitespace-pre-wrap text-sm leading-6 text-stone-950">
                  {viewModel.partnerAnswer.content}
                </p>
                {viewModel.partnerAnswer.updatedAt ? (
                  <p className="mt-4 text-xs text-stone-500">
                    마지막 수정 {formatDateTime(viewModel.partnerAnswer.updatedAt)}
                  </p>
                ) : null}
              </>
            ) : null}
          </div>
        </article>
      </div>
    </section>
  );
}
