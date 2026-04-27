import { AppLayout } from "@/components/layout/app-layout";
import { requireCoupleSetupUser } from "@/lib/routing/server";
import { CoupleClient } from "./couple-client";

export default async function CouplePage() {
  const user = await requireCoupleSetupUser();

  return (
    <AppLayout
      currentUser={user}
      title="커플 연결"
      description="초대 코드를 만들거나 입력해서 두 사람의 기록장을 연결해보세요."
    >
      <CoupleClient />
    </AppLayout>
  );
}
