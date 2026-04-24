import { AppLayout } from "@/components/layout/app-layout";
import { TodayQuestionPanel } from "@/features/today/today-question-panel";
import { requireProtectedUser } from "@/lib/routing/server";

export default async function TodayPage() {
  const user = await requireProtectedUser();

  return (
    <AppLayout
      currentUser={user}
      title="오늘 질문"
      description="오늘의 질문을 마주하고, 서로의 답변이 열리는 순간을 같은 기록장 안에서 확인해보세요."
    >
      <TodayQuestionPanel />
    </AppLayout>
  );
}
