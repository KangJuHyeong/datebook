type ExportTab = "select" | "orders";

type ExportTabsProps = {
  activeTab: ExportTab;
  onTabChange: (tab: ExportTab) => void;
};

export function ExportTabs({ activeTab, onTabChange }: ExportTabsProps) {
  return (
    <section className="rounded-lg border border-stone-200 bg-white p-2" aria-label="주문 화면 탭">
      <div className="grid gap-2 sm:grid-cols-2">
        <button
          type="button"
          onClick={() => onTabChange("select")}
          aria-pressed={activeTab === "select"}
          className={`min-h-11 rounded-md px-4 py-2 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-rose-100 ${
            activeTab === "select" ? "bg-rose-700 text-white" : "text-stone-700 hover:bg-stone-50"
          }`}
        >
          기록 선택
        </button>
        <button
          type="button"
          onClick={() => onTabChange("orders")}
          aria-pressed={activeTab === "orders"}
          className={`min-h-11 rounded-md px-4 py-2 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-rose-100 ${
            activeTab === "orders" ? "bg-rose-700 text-white" : "text-stone-700 hover:bg-stone-50"
          }`}
        >
          주문 내역
        </button>
      </div>
    </section>
  );
}
