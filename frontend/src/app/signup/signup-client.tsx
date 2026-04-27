"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { FormEvent } from "react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { getCurrentUser, signup } from "@/lib/api/auth";
import { submitSignup } from "@/features/auth/auth-form-logic.mjs";

type SignupFieldErrors = {
  email?: string;
  password?: string;
  displayName?: string;
};

export function SignupClient() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [fieldErrors, setFieldErrors] = useState<SignupFieldErrors>({});
  const [formError, setFormError] = useState<string>();
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setFieldErrors({});
    setFormError(undefined);

    const result = await submitSignup({ email, password, displayName }, { signup, getCurrentUser });

    if (!result.ok) {
      setFieldErrors(result.fieldErrors as SignupFieldErrors);
      setFormError(result.formError);
      setPending(false);
      return;
    }

    router.replace(result.redirectTo);
    router.refresh();
  }

  return (
    <section className="rounded-lg border border-stone-200 bg-white p-5">
      <div className="space-y-1">
        <h2 className="text-sm font-semibold text-stone-900">조용한 기록을 시작해요.</h2>
        <p className="text-sm leading-6 text-stone-500">가입 후 바로 커플 연결을 이어갈 수 있어요.</p>
      </div>
      <form className="mt-5 space-y-4" onSubmit={handleSubmit} noValidate>
        <Input
          id="displayName"
          name="displayName"
          label="표시 이름"
          type="text"
          autoComplete="nickname"
          placeholder="민지"
          maxLength={20}
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          error={fieldErrors.displayName}
        />
        <Input
          id="signup-email"
          name="email"
          label="이메일"
          type="email"
          autoComplete="email"
          placeholder="a@example.com"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={fieldErrors.email}
        />
        <Input
          id="signup-password"
          name="password"
          label="비밀번호"
          type="password"
          autoComplete="new-password"
          placeholder="8자 이상 입력해주세요."
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={fieldErrors.password}
        />
        {formError ? (
          <p className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700" role="alert">
            {formError}
          </p>
        ) : null}
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Button type="submit" disabled={pending} data-testid="signup-submit">
            {pending ? "가입 중..." : "가입하기"}
          </Button>
          <Link className="text-sm font-medium text-stone-500 hover:text-stone-900" href="/login">
            로그인으로 이동
          </Link>
        </div>
      </form>
    </section>
  );
}
