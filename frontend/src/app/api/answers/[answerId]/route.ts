import type { NextRequest } from "next/server";
import { proxyBackendRequest } from "@/lib/server/backend";

type RouteContext = {
  params: Promise<{
    answerId: string;
  }>;
};

export async function PUT(request: NextRequest, context: RouteContext) {
  const { answerId } = await context.params;
  return proxyBackendRequest(request, `/api/answers/${encodeURIComponent(answerId)}`);
}
