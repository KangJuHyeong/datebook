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
      setCopyFeedback("\ucf54\ub4dc\ub97c \ubcf5\uc0ac\ud588\uc5b4\uc694.");
    } catch {
      setCopyFeedback("\ubcf5\uc0ac\uc5d0 \uc2e4\ud328\ud588\uc5b4\uc694. \ucf54\ub4dc\ub97c \uc9c1\uc811 \uc120\ud0dd\ud574\uc8fc\uc138\uc694.");
    }
  }

  return (
    <section className="grid gap-4 md:grid-cols-2">
      <article className="rounded-lg border border-stone-200 bg-white p-5">
        <div className="space-y-3">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-sm font-semibold text-stone-900">\ucd08\ub300 \ucf54\ub4dc \ub9cc\ub4e4\uae30</h2>
            <StatusBadge tone={inviteState ? "progress" : "neutral"}>
              {inviteState ? "\uc0c1\ub300 \ub300\uae30" : "\uc900\ube44 \uc804"}
            </StatusBadge>
          </div>
          <p className="text-sm leading-6 text-stone-700">
            \uc0c1\ub300\uc5d0\uac8c \uc804\ub2ec\ud560 \ucf54\ub4dc\ub97c \ub9cc\ub4e4\uace0, \uc5f0\uacb0\uc774 \uc644\ub8cc\ub420 \ub54c\uae4c\uc9c0 \uc774 \ud654\uba74\uc5d0\uc11c \uae30\ub2e4\ub9b4 \uc218 \uc788\uc5b4\uc694.
          </p>
          {inviteState ? (
            <div className="space-y-3 rounded-lg border border-stone-200 bg-stone-50 p-4" aria-live="polite">
              <div className="space-y-1">
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-stone-500">Invite Code</p>
                <p className="font-mono text-xl text-stone-950">{inviteState.inviteCode}</p>
              </div>
              <p className="text-sm text-stone-700">\ub9cc\ub8cc \uc2dc\uac04: {formattedExpiry}</p>
              <p className="text-sm text-stone-500">\uc0c1\ub300\uac00 \uc774 \ucf54\ub4dc\ub97c \uc785\ub825\ud558\uba74 \uc624\ub298 \uc9c8\ubb38\uc73c\ub85c \ud568\uaed8 \uc774\ub3d9\ud560 \uc218 \uc788\uc5b4\uc694.</p>
              <div className="flex flex-wrap items-center gap-3">
                <Button type="button" variant="secondary" onClick={handleCopyInviteCode}>
                  \ucf54\ub4dc \ubcf5\uc0ac
                </Button>
                <StatusBadge tone="waiting">\uc9c8\ubb38 \uc5f4\uae30 \uc804 \ub300\uae30 \uc911</StatusBadge>
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
            {creating ? "\ucf54\ub4dc \ub9cc\ub4dc\ub294 \uc911..." : inviteState ? "\ucf54\ub4dc \uc0dd\uc131 \uc644\ub8cc" : "\ucf54\ub4dc \ub9cc\ub4e4\uae30"}
          </Button>
        </div>
      </article>
      <article className="rounded-lg border border-stone-200 bg-white p-5">
        <form className="space-y-3" onSubmit={handleJoin} noValidate>
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-sm font-semibold text-stone-900">\ucd08\ub300 \ucf54\ub4dc \uc785\ub825</h2>
            <StatusBadge tone="selected">\ubc14\ub85c \uc5f0\uacb0</StatusBadge>
          </div>
          <p className="text-sm leading-6 text-stone-700">\uc0c1\ub300\uac00 \ubcf4\ub0b8 \ucf54\ub4dc\ub97c \ubd99\uc5ec \ub123\uc73c\uba74 \ub450 \uc0ac\ub78c\uc758 \uae30\ub85d\uc7a5\uc774 \uc5f0\uacb0\ub3fc\uc694.</p>
          <Input
            id="inviteCode"
            name="inviteCode"
            label="\ucd08\ub300 \ucf54\ub4dc"
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
            {joining ? "\ucc38\uc5ec \uc911..." : "\ucc38\uc5ec\ud558\uae30"}
          </Button>
        </form>
      </article>
    </section>
  );
}
