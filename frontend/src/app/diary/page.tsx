import { AppLayout } from "@/components/layout/app-layout";
import { DiaryEntriesPanel } from "@/features/diary/diary-entries-panel";
import { requireProtectedUser } from "@/lib/routing/server";

export default async function DiaryPage() {
  const user = await requireProtectedUser();

  return (
    <AppLayout
      currentUser={user}
      title="기록"
      description="날짜순으로 쌓인 질문과 답변 공개 상태를 차분하게 살펴보세요."
    >
      <DiaryEntriesPanel />
    </AppLayout>
  );
}
