export default function TodayLoading() {
  return (
    <section className="space-y-4" aria-label="오늘 질문 로딩">
      <div className="min-h-40 animate-pulse rounded-lg border border-stone-200 bg-white p-5">
        <div className="h-4 w-24 rounded bg-stone-200" />
        <div className="mt-4 h-8 w-4/5 rounded bg-stone-200" />
        <div className="mt-3 h-4 w-2/3 rounded bg-stone-100" />
      </div>
      <div className="grid gap-4 md:grid-cols-2">
        <div className="min-h-72 animate-pulse rounded-lg border border-stone-200 bg-white p-5" />
        <div className="min-h-72 animate-pulse rounded-lg border border-stone-200 bg-white p-5" />
      </div>
    </section>
  );
}
