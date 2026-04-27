import type { NextRequest } from "next/server";
import { proxyBackendRequest } from "@/lib/server/backend";

export function POST(request: NextRequest) {
  return proxyBackendRequest(request, "/api/couples");
}
