import type { ReactNode } from "react";
import type { AuthUser, AnswerState } from "./api";

export type NavItem = {
  href: string;
  label: string;
};

export type AppLayoutProps = {
  title: string;
  description?: string;
  currentUser?: AuthUser | null;
  children: ReactNode;
};

export type StatusTone = "neutral" | "success" | "waiting" | "progress" | "error" | "selected";

export type RouteTarget = "/login" | "/couple" | "/today";

export type ProtectedRouteResolution =
  | { allowed: true; user: AuthUser }
  | { allowed: false; redirectTo: RouteTarget };

export const ANSWER_STATE_COPY: Record<AnswerState, string> = {
  NOT_ANSWERED: "오늘의 답변을 남겨보세요.",
  MY_ANSWERED_PARTNER_WAITING: "내 답변은 저장됐어요. 상대가 답하면 함께 열려요.",
  PARTNER_ANSWERED_ME_WAITING: "상대가 답변을 마쳤어요. 내 답변을 남기면 함께 열려요.",
  BOTH_ANSWERED: "두 사람의 답변이 열렸어요.",
};
