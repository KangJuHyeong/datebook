import type { NavItem, StatusTone } from "@/types/app";

export const APP_NAV_ITEMS: NavItem[] = [
  { href: "/today", label: "\uc624\ub298 \uc9c8\ubb38" },
  { href: "/diary", label: "\uae30\ub85d" },
  { href: "/export", label: "\uc8fc\ubb38" },
];

export const STATUS_BADGE_STYLES: Record<StatusTone, string> = {
  neutral: "border-stone-200 bg-stone-100 text-stone-600",
  success: "border-emerald-200 bg-emerald-50 text-emerald-700",
  waiting: "border-stone-200 bg-stone-100 text-stone-600",
  progress: "border-amber-200 bg-amber-50 text-amber-800",
  error: "border-red-200 bg-red-50 text-red-700",
  selected: "border-rose-200 bg-rose-50 text-rose-800",
};
