import { AppLayout } from "@/components/layout/app-layout";
import { PlaceholderPanel } from "@/features/shell/placeholder-panel";
import { requireProtectedUser } from "@/lib/routing/server";

export default async function DiaryPage() {
  const user = await requireProtectedUser();

  return (
    <AppLayout
      currentUser={user}
      title="기록"
      description="기록 목록 조회와 상세 상태 렌더링은 다음 step에서 구현합니다."
    >
      <PlaceholderPanel
        eyebrow="Diary"
        title="쌓인 기록을 보여줄 자리입니다."
        description="미로그인 사용자는 로그인으로, 커플 연결 전 사용자는 연결 화면으로 이동하도록 보호 라우트를 먼저 구성했습니다."
        badge="보호됨"
        tone="progress"
      />
    </AppLayout>
  );
}
