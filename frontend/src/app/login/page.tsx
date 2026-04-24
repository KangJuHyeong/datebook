import Link from "next/link";
import { AppLayout } from "@/components/layout/app-layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export default function LoginPage() {
  return (
    <AppLayout title="다시 기록장으로" description="로그인 폼 상세 동작은 다음 step에서 연결합니다.">
      <section className="rounded-lg border border-stone-200 bg-white p-5">
        <form className="space-y-4">
          <Input id="email" label="이메일" type="email" placeholder="a@example.com" />
          <Input id="password" label="비밀번호" type="password" placeholder="비밀번호를 입력하세요" />
          <div className="flex flex-wrap items-center justify-between gap-3">
            <Button type="button">로그인</Button>
            <Link className="text-sm font-medium text-stone-500 hover:text-stone-900" href="/signup">
              회원가입으로 이동
            </Link>
          </div>
        </form>
      </section>
    </AppLayout>
  );
}
