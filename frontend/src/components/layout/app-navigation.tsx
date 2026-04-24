import Link from "next/link";
import { APP_NAV_ITEMS } from "@/lib/constants/ui";
import type { AuthUser } from "@/types/api";

type AppNavigationProps = {
  currentUser?: AuthUser | null;
};

export function AppNavigation({ currentUser }: AppNavigationProps) {
  return (
    <header className="border-b border-stone-200 bg-stone-50/95">
      <div className="mx-auto flex max-w-3xl items-center justify-between gap-4 px-4 py-4">
        <div className="space-y-1">
          <p className="text-xs font-medium uppercase tracking-[0.18em] text-stone-500">Question Diary</p>
          <p className="text-sm text-stone-700">하루 한 질문 교환일기</p>
        </div>
        <nav aria-label="주요 메뉴" className="flex items-center gap-2">
          {APP_NAV_ITEMS.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="rounded-md px-3 py-2 text-sm font-medium text-stone-600 transition-colors hover:bg-white hover:text-stone-950"
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </div>
      {currentUser ? (
        <div className="mx-auto max-w-3xl px-4 pb-4 text-sm text-stone-500">
          {currentUser.displayName}
          {currentUser.coupleId ? " 님의 기록장" : " 님, 커플 연결을 기다리고 있어요."}
        </div>
      ) : null}
    </header>
  );
}
