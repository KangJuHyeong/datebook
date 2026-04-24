"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { logout } from "@/lib/api/auth";

export function LogoutButton() {
  const router = useRouter();
  const [pending, setPending] = useState(false);

  async function handleLogout() {
    setPending(true);

    try {
      await logout();
    } finally {
      router.replace("/login");
      router.refresh();
      setPending(false);
    }
  }

  return (
    <Button type="button" variant="secondary" className="px-3" onClick={handleLogout} disabled={pending}>
      {pending ? "\ub85c\uadf8\uc544\uc6c3 \uc911..." : "\ub85c\uadf8\uc544\uc6c3"}
    </Button>
  );
}
