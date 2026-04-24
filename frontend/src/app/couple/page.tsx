import { AppLayout } from "@/components/layout/app-layout";
import { CoupleConnectPanel } from "@/features/couple/couple-connect-panel";
import { requireCoupleSetupUser } from "@/lib/routing/server";

export default async function CouplePage() {
  const user = await requireCoupleSetupUser();

  return (
    <AppLayout
      currentUser={user}
      title="\ucee4\ud50c \uc5f0\uacb0"
      description="\ucd08\ub300 \ucf54\ub4dc\ub97c \ub9cc\ub4e4\uac70\ub098 \uc785\ub825\ud574\uc11c \ub450 \uc0ac\ub78c\uc758 \uae30\ub85d\uc7a5\uc744 \uc5f0\uacb0\ud574\ubcf4\uc138\uc694."
    >
      <CoupleConnectPanel />
    </AppLayout>
  );
}
