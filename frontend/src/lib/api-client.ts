import { ApiError } from "./api-error";
import { getCsrfToken } from "./csrf";

const API_BASE = process.env.NEXT_PUBLIC_BFF_URL || "http://localhost:9010";

const MUTATING_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

export async function apiFetch<T>(
  path: string,
  options?: RequestInit,
): Promise<T> {
  const method = options?.method?.toUpperCase() ?? "GET";

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options?.headers as Record<string, string>),
  };

  if (MUTATING_METHODS.has(method)) {
    const csrfToken = getCsrfToken();
    if (csrfToken) {
      headers["X-XSRF-TOKEN"] = csrfToken;
    }
  }

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    method,
    credentials: "include",
    headers,
  });

  if (res.status === 401) {
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new ApiError(401, "Session expired");
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(res.status, body.error || `Request failed (${res.status})`);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  return res.json();
}

export function apiGet<T>(path: string): Promise<T> {
  return apiFetch<T>(path);
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return apiFetch<T>(path, {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function apiPut<T>(path: string, body: unknown): Promise<T> {
  return apiFetch<T>(path, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export function apiPatch<T>(path: string, body: unknown): Promise<T> {
  return apiFetch<T>(path, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export function apiDelete<T = void>(path: string): Promise<T> {
  return apiFetch<T>(path, { method: "DELETE" });
}
