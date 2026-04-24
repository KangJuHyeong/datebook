import { AppNavigation } from "./app-navigation";
import type { AppLayoutProps } from "@/types/app";

export function AppLayout({ title, description, currentUser, children }: AppLayoutProps) {
  return (
    <div className="min-h-screen bg-stone-50">
      <AppNavigation currentUser={currentUser} />
      <main className="mx-auto max-w-3xl px-4 py-6 md:py-10">
        <section className="space-y-6">
          <header className="space-y-2">
            <h1 className="text-2xl font-semibold text-stone-950">{title}</h1>
            {description ? <p className="text-sm leading-6 text-stone-700">{description}</p> : null}
          </header>
          {children}
        </section>
      </main>
    </div>
  );
}
