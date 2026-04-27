"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import type { FormEvent } from "react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { login } from "@/lib/api/auth";
import { submitLogin } from "@/features/auth/auth-form-logic.mjs";

type LoginFieldErrors = {
  email?: string;
  password?: string;
};

export function LoginClient() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [fieldErrors, setFieldErrors] = useState<LoginFieldErrors>({});
  const [formError, setFormError] = useState<string>();
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setFieldErrors({});
    setFormError(undefined);

    const result = await submitLogin({ email, password }, { login });

    if (!result.ok) {
      setFieldErrors(result.fieldErrors as LoginFieldErrors);
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
        <h2 className="text-sm font-semibold text-stone-900">다시 기록장으로 돌아와요.</h2>
        <p className="text-sm leading-6 text-stone-500">오늘의 질문과 두 사람의 기록을 이어서 확인할 수 있어요.</p>
      </div>
      <form className="mt-5 space-y-4" onSubmit={handleSubmit} noValidate>
        <Input
          id="login-email"
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
          id="login-password"
          name="password"
          label="비밀번호"
          type="password"
          autoComplete="current-password"
          placeholder="비밀번호를 입력해주세요."
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
          <Button type="submit" disabled={pending} data-testid="login-submit">
            {pending ? "로그인 중..." : "로그인"}
          </Button>
          <Link className="text-sm font-medium text-stone-500 hover:text-stone-900" href="/signup">
            회원가입으로 이동
          </Link>
        </div>
      </form>
    </section>
  );
}
