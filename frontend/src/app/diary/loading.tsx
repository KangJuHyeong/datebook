export default function DiaryLoading() {
  return (
    <section className="space-y-4" aria-label="기록 로딩">
      {Array.from({ length: 3 }).map((_, index) => (
        <div key={index} className="min-h-44 animate-pulse rounded-lg border border-stone-200 bg-white p-5" />
      ))}
    </section>
  );
}
