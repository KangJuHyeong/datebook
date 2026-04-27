import { AppLayout } from "@/components/layout/app-layout";
import { redirectAuthenticatedUser } from "@/lib/routing/server";
import { SignupClient } from "./signup-client";

export default async function SignupPage() {
  await redirectAuthenticatedUser();

  return (
    <AppLayout
      title="처음 기록을 시작해요."
      description="가입을 마치면 커플 연결을 만들거나 초대 코드를 입력할 수 있어요."
    >
      <SignupClient />
    </AppLayout>
  );
}
