import { AppLayout } from "@/components/layout/app-layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";

export default function CouplePage() {
  return (
    <AppLayout
      title="커플 연결"
      description="초대 코드 생성과 참여 동작은 다음 step에서 구현하고, 이 화면은 연결 전 셸 역할만 담당합니다."
    >
      <section className="grid gap-4 md:grid-cols-2">
        <article className="rounded-lg border border-stone-200 bg-white p-5">
          <div className="space-y-3">
            <h2 className="text-sm font-semibold text-stone-900">초대 코드 만들기</h2>
            <p className="text-sm leading-6 text-stone-700">상대에게 전달할 코드를 만들고, 연결이 완료될 때까지 대기합니다.</p>
            <Button type="button">코드 만들기</Button>
          </div>
        </article>
        <article className="rounded-lg border border-stone-200 bg-white p-5">
          <div className="space-y-3">
            <h2 className="text-sm font-semibold text-stone-900">초대 코드 입력</h2>
            <Input id="inviteCode" label="초대 코드" placeholder="A1B2C3D4" />
            <Button type="button">참여하기</Button>
          </div>
        </article>
      </section>
    </AppLayout>
  );
}
