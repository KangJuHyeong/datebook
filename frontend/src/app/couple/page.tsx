import { AppLayout } from "@/components/layout/app-layout";
import { CoupleConnectPanel } from "@/features/couple/couple-connect-panel";
import { requireCoupleSetupUser } from "@/lib/routing/server";

export default async function CouplePage() {
  const user = await requireCoupleSetupUser();

  return (
    <AppLayout
      currentUser={user}
      title="커플 연결"
      description="초대 코드를 만들거나 입력해서 두 사람의 기록장을 연결해보세요."
    >
      <CoupleConnectPanel />
    </AppLayout>
  );
}
