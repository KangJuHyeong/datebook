import { AppLayout } from "@/components/layout/app-layout";
import { requireProtectedUser } from "@/lib/routing/server";
import { ExportClient } from "./export-client";

export default async function ExportPage() {
  const user = await requireProtectedUser();

  return (
    <AppLayout
      currentUser={user}
      title="주문"
      description="기록을 고르고, 주문 미리보기로 확인한 뒤, 주문 완료 후 다운로드할 수 있어요."
    >
      <ExportClient />
    </AppLayout>
  );
}
