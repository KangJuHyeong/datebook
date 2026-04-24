import { redirect } from "next/navigation";
import { getRootRedirectPath } from "@/lib/routing/server";

export default async function Home() {
  redirect(await getRootRedirectPath());
}
