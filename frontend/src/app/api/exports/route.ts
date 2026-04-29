import type { NextRequest } from "next/server";
import { proxyBackendRequest } from "@/lib/server/backend";

export function GET(request: NextRequest) {
  return proxyBackendRequest(request, "/api/exports");
}

export function POST(request: NextRequest) {
  return proxyBackendRequest(request, "/api/exports");
}
