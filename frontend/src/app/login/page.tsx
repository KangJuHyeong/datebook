import { AppLayout } from "@/components/layout/app-layout";
import { AuthForm } from "@/features/auth/auth-form";
import { redirectAuthenticatedUser } from "@/lib/routing/server";

export default async function LoginPage() {
  await redirectAuthenticatedUser();

  return (
    <AppLayout
      title="\ub2e4\uc2dc \uae30\ub85d\uc7a5\uc73c\ub85c \ub3cc\uc544\uc640\uc694."
      description="\uc774\uba54\uc77c\uacfc \ube44\ubc00\ubc88\ud638\ub97c \uc785\ub825\ud558\uba74 \uc624\ub298\uc758 \uc9c8\ubb38\uc73c\ub85c \uc774\uc5b4\uc11c \ub4e4\uc5b4\uac08 \uc218 \uc788\uc5b4\uc694."
    >
      <AuthForm mode="login" />
    </AppLayout>
  );
}
