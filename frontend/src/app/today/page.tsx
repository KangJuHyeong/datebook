import { AppLayout } from "@/components/layout/app-layout";
import { PlaceholderPanel } from "@/features/shell/placeholder-panel";
import { requireProtectedUser } from "@/lib/routing/server";

export default async function TodayPage() {
  const user = await requireProtectedUser();

  return (
    <AppLayout
      currentUser={user}
      title="오늘 질문"
      description="오늘의 질문 조회와 답변 작성 UI는 다음 step에서 연결합니다."
    >
      <PlaceholderPanel
        eyebrow="Today"
        title="오늘의 질문을 불러올 준비가 됐어요."
        description="이 step에서는 로그인과 커플 연결 상태를 먼저 확인하고, 상세 질문 카드와 답변 폼은 이후 step에서 이어집니다."
        badge="가드 완료"
        tone="success"
      />
    </AppLayout>
  );
}
