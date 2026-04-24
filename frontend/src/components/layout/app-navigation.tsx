import Link from "next/link";
import { LogoutButton } from "@/features/auth/logout-button";
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
          <p className="text-sm text-stone-700">\ud558\ub8e8 \ud55c \uc9c8\ubb38 \uad50\ud658\uc77c\uae30</p>
        </div>
        {currentUser ? (
          <div className="flex items-center gap-2">
            <nav aria-label="\uc8fc\uc694 \uba54\ub274" className="flex items-center gap-2">
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
            <LogoutButton />
          </div>
        ) : (
          <nav aria-label="\uc778\uc99d \uba54\ub274" className="flex items-center gap-2">
            <Link
              href="/login"
              className="rounded-md px-3 py-2 text-sm font-medium text-stone-600 transition-colors hover:bg-white hover:text-stone-950"
            >
              \ub85c\uadf8\uc778
            </Link>
            <Link
              href="/signup"
              className="rounded-md px-3 py-2 text-sm font-medium text-stone-600 transition-colors hover:bg-white hover:text-stone-950"
            >
              \ud68c\uc6d0\uac00\uc785
            </Link>
          </nav>
        )}
      </div>
      {currentUser ? (
        <div className="mx-auto max-w-3xl px-4 pb-4 text-sm text-stone-500">
          {currentUser.displayName}
          {currentUser.coupleId ? " \ub2d8\uc758 \uae30\ub85d\uc7a5" : " \ub2d8, \ucee4\ud50c \uc5f0\uacb0\uc744 \uae30\ub2e4\ub9ac\uace0 \uc788\uc5b4\uc694."}
        </div>
      ) : null}
    </header>
  );
}
