import type { NextRequest } from "next/server";
import { proxyBackendRequest } from "@/lib/server/backend";

type RouteContext = {
  params: Promise<{
    exportRequestId: string;
  }>;
};

export async function POST(request: NextRequest, context: RouteContext) {
  const { exportRequestId } = await context.params;
  return proxyBackendRequest(request, `/api/exports/${encodeURIComponent(exportRequestId)}/preview`);
}
