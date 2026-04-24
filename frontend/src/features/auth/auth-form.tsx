"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { getCurrentUser, login, signup } from "@/lib/api/auth";
import { submitLogin, submitSignup } from "./auth-form-logic.mjs";

type AuthMode = "login" | "signup";

type AuthFieldErrors = {
  email?: string;
  password?: string;
  displayName?: string;
};

type AuthFormProps = {
  mode: AuthMode;
};

const FORM_COPY = {
  login: {
    title: "다시 기록장으로 돌아와요.",
    description: "오늘의 질문과 두 사람의 기록을 이어서 확인할 수 있어요.",
    submitLabel: "로그인",
    pendingLabel: "로그인 중...",
    alternateLabel: "회원가입으로 이동",
    alternateHref: "/signup",
  },
  signup: {
    title: "조용한 기록을 시작해요.",
    description: "가입 후 바로 커플 연결을 이어갈 수 있어요.",
    submitLabel: "가입하기",
    pendingLabel: "가입 중...",
    alternateLabel: "로그인으로 이동",
    alternateHref: "/login",
  },
} as const;

export function AuthForm({ mode }: AuthFormProps) {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [fieldErrors, setFieldErrors] = useState<AuthFieldErrors>({});
  const [formError, setFormError] = useState<string>();
  const [pending, setPending] = useState(false);
  const copy = FORM_COPY[mode];

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setFieldErrors({});
    setFormError(undefined);

    const result =
      mode === "login"
        ? await submitLogin({ email, password }, { login })
        : await submitSignup({ email, password, displayName }, { signup, getCurrentUser });

    if (!result.ok) {
      setFieldErrors(result.fieldErrors as AuthFieldErrors);
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
        <h2 className="text-sm font-semibold text-stone-900">{copy.title}</h2>
        <p className="text-sm leading-6 text-stone-500">{copy.description}</p>
      </div>
      <form className="mt-5 space-y-4" onSubmit={handleSubmit} noValidate>
        {mode === "signup" ? (
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
        ) : null}
        <Input
          id={`${mode}-email`}
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
          id={`${mode}-password`}
          name="password"
          label="비밀번호"
          type="password"
          autoComplete={mode === "login" ? "current-password" : "new-password"}
          placeholder={mode === "login" ? "비밀번호를 입력해주세요." : "8자 이상 입력해주세요."}
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
          <Button type="submit" disabled={pending}>
            {pending ? copy.pendingLabel : copy.submitLabel}
          </Button>
          <Link className="text-sm font-medium text-stone-500 hover:text-stone-900" href={copy.alternateHref}>
            {copy.alternateLabel}
          </Link>
        </div>
      </form>
    </section>
  );
}
