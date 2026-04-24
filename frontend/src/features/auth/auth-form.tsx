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
    title: "\ub2e4\uc2dc \uae30\ub85d\uc7a5\uc73c\ub85c \ub3cc\uc544\uc640\uc694.",
    description: "\uc624\ub298\uc758 \uc9c8\ubb38\uacfc \ub450 \uc0ac\ub78c\uc758 \uae30\ub85d\uc744 \uc774\uc5b4\uc11c \ud655\uc778\ud560 \uc218 \uc788\uc5b4\uc694.",
    submitLabel: "\ub85c\uadf8\uc778",
    pendingLabel: "\ub85c\uadf8\uc778 \uc911...",
    alternateLabel: "\ud68c\uc6d0\uac00\uc785\uc73c\ub85c \uc774\ub3d9",
    alternateHref: "/signup",
  },
  signup: {
    title: "\uc870\uc6a9\ud55c \uae30\ub85d\uc744 \uc2dc\uc791\ud574\uc694.",
    description: "\uac00\uc785 \ud6c4 \ubc14\ub85c \ucee4\ud50c \uc5f0\uacb0\uc744 \uc774\uc5b4\uac08 \uc218 \uc788\uc5b4\uc694.",
    submitLabel: "\uac00\uc785\ud558\uae30",
    pendingLabel: "\uac00\uc785 \uc911...",
    alternateLabel: "\ub85c\uadf8\uc778\uc73c\ub85c \uc774\ub3d9",
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
            label="\ud45c\uc2dc \uc774\ub984"
            type="text"
            autoComplete="nickname"
            placeholder="\ubbfc\uc9c0"
            maxLength={20}
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            error={fieldErrors.displayName}
          />
        ) : null}
        <Input
          id={`${mode}-email`}
          name="email"
          label="\uc774\uba54\uc77c"
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
          label="\ube44\ubc00\ubc88\ud638"
          type="password"
          autoComplete={mode === "login" ? "current-password" : "new-password"}
          placeholder={mode === "login" ? "\ube44\ubc00\ubc88\ud638\ub97c \uc785\ub825\ud574\uc8fc\uc138\uc694." : "8\uc790 \uc774\uc0c1 \uc785\ub825\ud574\uc8fc\uc138\uc694."}
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
