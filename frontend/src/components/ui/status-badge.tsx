import type { ReactNode } from "react";
import { STATUS_BADGE_STYLES } from "@/lib/constants/ui";
import type { StatusTone } from "@/types/app";

type StatusBadgeProps = {
  tone?: StatusTone;
  children: ReactNode;
};

export function StatusBadge({ tone = "neutral", children }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex rounded-full border px-2.5 py-1 text-xs font-medium ${STATUS_BADGE_STYLES[tone]}`.trim()}
      aria-label={typeof children === "string" ? children : "상태"}
    >
      {children}
    </span>
  );
}
