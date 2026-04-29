import type { NextRequest } from "next/server";

const BACKEND_API_BASE_URL =
  process.env.API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

function buildBackendUrl(path: string, search: string) {
  return `${BACKEND_API_BASE_URL}${path}${search}`;
}

function buildForwardHeaders(request: NextRequest) {
  const headers = new Headers();
  const allowedRequestHeaders = ["accept", "content-type", "cookie", "x-xsrf-token"];

  for (const headerName of allowedRequestHeaders) {
    const value = request.headers.get(headerName);
    if (value) {
      headers.set(headerName, value);
    }
  }

  return headers;
}

function buildResponseHeaders(response: Response) {
  const headers = new Headers();
  const allowedResponseHeaders = ["content-type", "content-disposition", "cache-control"];

  for (const headerName of allowedResponseHeaders) {
    const value = response.headers.get(headerName);
    if (value) {
      headers.set(headerName, value);
    }
  }

  const responseHeaders = response.headers as Headers & { getSetCookie?: () => string[] };
  const setCookies = responseHeaders.getSetCookie?.() ?? [];
  if (setCookies.length) {
    for (const cookie of setCookies) {
      headers.append("set-cookie", cookie);
    }
  } else {
    const setCookie = response.headers.get("set-cookie");
    if (setCookie) {
      headers.set("set-cookie", setCookie);
    }
  }

  return headers;
}

export async function proxyBackendRequest(request: NextRequest, backendPath: string) {
  const method = request.method.toUpperCase();
  const hasBody = method !== "GET" && method !== "HEAD";

  const response = await fetch(buildBackendUrl(backendPath, request.nextUrl.search), {
    method,
    headers: buildForwardHeaders(request),
    body: hasBody ? await request.arrayBuffer() : undefined,
    redirect: "manual",
    cache: "no-store",
  });

  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers: buildResponseHeaders(response),
  });
}
