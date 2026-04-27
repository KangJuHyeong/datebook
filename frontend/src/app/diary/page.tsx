import { AppLayout } from "@/components/layout/app-layout";
import { requireProtectedUser } from "@/lib/routing/server";
import { DiaryClient } from "./diary-client";

export default async function DiaryPage() {
  const user = await requireProtectedUser();

  return (
    <AppLayout
      currentUser={user}
      title="기록"
      description="날짜순으로 쌓인 질문과 답변 공개 상태를 차분하게 살펴보세요."
    >
      <DiaryClient />
    </AppLayout>
  );
}
