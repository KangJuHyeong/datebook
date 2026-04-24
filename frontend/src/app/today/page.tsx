import { AppLayout } from "@/components/layout/app-layout";
import { TodayQuestionPanel } from "@/features/today/today-question-panel";
import { requireProtectedUser } from "@/lib/routing/server";

export default async function TodayPage() {
  const user = await requireProtectedUser();

  return (
    <AppLayout
      currentUser={user}
      title="오늘 질문"
      description="오늘의 질문에 답하고, 서로의 답변이 언제 열리는지 같은 흐름 안에서 확인해보세요."
    >
      <TodayQuestionPanel />
    </AppLayout>
  );
}
