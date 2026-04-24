import { AppLayout } from "@/components/layout/app-layout";
import { PlaceholderPanel } from "@/features/shell/placeholder-panel";
import { requireProtectedUser } from "@/lib/routing/server";

export default async function ExportPage() {
  const user = await requireProtectedUser();

  return (
    <AppLayout
      currentUser={user}
      title="주문"
      description="기록 선택, 주문 미리보기, 다운로드 버튼은 다음 step에서 단계별로 구현합니다."
    >
      <PlaceholderPanel
        eyebrow="Order"
        title="둘이 함께 남긴 기록을 주문할 준비를 하고 있어요."
        description="이 화면에서는 주문, 기록 선택, 다운로드 흐름만 보여주고 상세 단계 연결은 다음 step에서 이어집니다."
        badge="셸 준비"
        tone="selected"
      />
    </AppLayout>
  );
}
