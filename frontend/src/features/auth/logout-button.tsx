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
      {pending ? "로그아웃 중..." : "로그아웃"}
    </Button>
  );
}
