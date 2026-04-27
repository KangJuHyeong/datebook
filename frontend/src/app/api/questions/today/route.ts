import type { NextRequest } from "next/server";
import { proxyBackendRequest } from "@/lib/server/backend";

export function GET(request: NextRequest) {
  return proxyBackendRequest(request, "/api/questions/today");
}
