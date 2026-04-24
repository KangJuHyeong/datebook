import { StatusBadge } from "@/components/ui/status-badge";
import type { StatusTone } from "@/types/app";

type PlaceholderPanelProps = {
  eyebrow: string;
  title: string;
  description: string;
  badge: string;
  tone?: StatusTone;
};

export function PlaceholderPanel({
  eyebrow,
  title,
  description,
  badge,
  tone = "waiting",
}: PlaceholderPanelProps) {
  return (
    <section className="rounded-lg border border-stone-200 bg-white p-5">
      <div className="space-y-3">
        <p className="text-xs uppercase tracking-[0.16em] text-stone-500">{eyebrow}</p>
        <div className="flex flex-wrap items-center gap-3">
          <h2 className="text-xl font-medium leading-relaxed text-stone-950">{title}</h2>
          <StatusBadge tone={tone}>{badge}</StatusBadge>
        </div>
        <p className="text-sm leading-6 text-stone-700">{description}</p>
      </div>
    </section>
  );
}
