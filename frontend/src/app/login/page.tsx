import { AppLayout } from "@/components/layout/app-layout";
import { AuthForm } from "@/features/auth/auth-form";
import { redirectAuthenticatedUser } from "@/lib/routing/server";

export default async function LoginPage() {
  await redirectAuthenticatedUser();

  return (
    <AppLayout
      title="다시 기록장으로 돌아와요."
      description="이메일과 비밀번호를 입력하면 오늘의 질문으로 이어서 들어갈 수 있어요."
    >
      <AuthForm mode="login" />
    </AppLayout>
  );
}
