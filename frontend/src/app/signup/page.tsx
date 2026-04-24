import { AppLayout } from "@/components/layout/app-layout";
import { AuthForm } from "@/features/auth/auth-form";
import { redirectAuthenticatedUser } from "@/lib/routing/server";

export default async function SignupPage() {
  await redirectAuthenticatedUser();

  return (
    <AppLayout
      title="\ucc98\uc74c \uae30\ub85d\uc744 \uc2dc\uc791\ud574\uc694."
      description="\uac00\uc785\uc744 \ub9c8\uce58\uba74 \ucee4\ud50c \uc5f0\uacb0\uc744 \ub9cc\ub4e4\uac70\ub098 \ucd08\ub300 \ucf54\ub4dc\ub97c \uc785\ub825\ud560 \uc218 \uc788\uc5b4\uc694."
    >
      <AuthForm mode="signup" />
    </AppLayout>
  );
}
