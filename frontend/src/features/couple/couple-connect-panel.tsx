"use client";

import { useRouter } from "next/navigation";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { StatusBadge } from "@/components/ui/status-badge";
import { createCouple, joinCouple } from "@/lib/api/couple";
import { submitCreateInvite, submitJoinInvite } from "./couple-form-logic.mjs";

type CoupleFieldErrors = {
  inviteCode?: string;
};

type InviteState = {
  inviteCode: string;
  expiresAt: string;
};

function formatInviteExpiry(expiresAt: string) {
  return new Intl.DateTimeFormat("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(expiresAt));
}

export function CoupleConnectPanel() {
  const router = useRouter();
  const [inviteCode, setInviteCode] = useState("");
  const [inviteState, setInviteState] = useState<InviteState | null>(null);
  const [joinErrors, setJoinErrors] = useState<CoupleFieldErrors>({});
  const [createError, setCreateError] = useState<string>();
  const [joinError, setJoinError] = useState<string>();
  const [copyFeedback, setCopyFeedback] = useState<string>();
  const [creating, setCreating] = useState(false);
  const [joining, setJoining] = useState(false);

  const formattedExpiry = useMemo(
    () => (inviteState ? formatInviteExpiry(inviteState.expiresAt) : ""),
    [inviteState],
  );

  async function handleCreateInvite() {
    setCreating(true);
    setCreateError(undefined);
    setCopyFeedback(undefined);

    const result = await submitCreateInvite({ createCouple });

    if (!result.ok) {
      setCreateError(result.message);
      setCreating(false);
      return;
    }

    setInviteState({
      inviteCode: result.invite.inviteCode,
      expiresAt: result.invite.expiresAt,
    });
    setCreating(false);
  }

  async function handleJoin(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setJoining(true);
    setJoinError(undefined);

    const result = await submitJoinInvite({ inviteCode }, { joinCouple });

    if (!result.ok) {
      setJoinErrors(result.fieldErrors as CoupleFieldErrors);
      setJoinError(result.formError);
      setJoining(false);
      return;
    }

    setJoinErrors({});
    router.replace(result.redirectTo);
    router.refresh();
  }

  async function handleCopyInviteCode() {
    if (!inviteState) {
      return;
    }

    try {
      await navigator.clipboard.writeText(inviteState.inviteCode);
      setCopyFeedback("코드를 복사했어요.");
    } catch {
      setCopyFeedback("복사에 실패했어요. 코드를 직접 선택해주세요.");
    }
  }

  return (
    <section className="grid gap-4 md:grid-cols-2">
      <article className="rounded-lg border border-stone-200 bg-white p-5">
        <div className="space-y-3">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-sm font-semibold text-stone-900">초대 코드 만들기</h2>
            <StatusBadge tone={inviteState ? "progress" : "neutral"}>
              {inviteState ? "상대 대기" : "준비 전"}
            </StatusBadge>
          </div>
          <p className="text-sm leading-6 text-stone-700">
            상대에게 전달할 코드를 만들고, 연결이 완료될 때까지 이 화면에서 기다릴 수 있어요.
          </p>
          {inviteState ? (
            <div className="space-y-3 rounded-lg border border-stone-200 bg-stone-50 p-4" aria-live="polite">
              <div className="space-y-1">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-stone-500">Invite Code</p>
                <p className="font-mono text-xl text-stone-950">{inviteState.inviteCode}</p>
              </div>
              <p className="text-sm text-stone-700">만료 시간: {formattedExpiry}</p>
              <p className="text-sm text-stone-500">상대가 이 코드를 입력하면 오늘 질문으로 함께 이동할 수 있어요.</p>
              <div className="flex flex-wrap items-center gap-3">
                <Button type="button" variant="secondary" onClick={handleCopyInviteCode}>
                  코드 복사
                </Button>
                <StatusBadge tone="waiting">질문 열기 전 대기 중</StatusBadge>
              </div>
              {copyFeedback ? <p className="text-sm text-stone-500">{copyFeedback}</p> : null}
            </div>
          ) : null}
          {createError ? (
            <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {createError}
            </p>
          ) : null}
          <Button type="button" onClick={handleCreateInvite} disabled={creating || Boolean(inviteState)}>
            {creating ? "코드 만드는 중..." : inviteState ? "코드 생성 완료" : "코드 만들기"}
          </Button>
        </div>
      </article>
      <article className="rounded-lg border border-stone-200 bg-white p-5">
        <form className="space-y-3" onSubmit={handleJoin} noValidate>
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-sm font-semibold text-stone-900">초대 코드 입력</h2>
            <StatusBadge tone="selected">바로 연결</StatusBadge>
          </div>
          <p className="text-sm leading-6 text-stone-700">상대가 보낸 코드를 붙여 넣으면 두 사람의 기록장이 연결돼요.</p>
          <Input
            id="inviteCode"
            name="inviteCode"
            label="초대 코드"
            placeholder="A1B2C3D4"
            autoCapitalize="characters"
            value={inviteCode}
            onChange={(event) => setInviteCode(event.target.value.toUpperCase())}
            error={joinErrors.inviteCode}
          />
          {joinError ? (
            <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
              {joinError}
            </p>
          ) : null}
          <Button type="submit" disabled={joining}>
            {joining ? "참여 중..." : "참여하기"}
          </Button>
        </form>
      </article>
    </section>
  );
}
