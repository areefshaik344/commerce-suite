/**
 * Real HTTP client (axios) for the Spring Boot backend.
 *
 * - Reads base URL from `import.meta.env.VITE_API_URL`.
 * - Injects `Authorization: Bearer <accessToken>` on every request.
 * - Propagates `X-Request-Id` for tracing (see src/lib/requestId.ts).
 * - On 401, performs a single refresh-then-retry using `/auth/refresh`.
 * - Unwraps the backend envelope `{ success, data, message, timestamp }`
 *   into the frontend-facing `{ data, status, message }` shape that the
 *   existing UI code already expects, so downstream call-sites stay stable.
 *
 * NOTE: This client coexists with the legacy mock transport in `apiClient.ts`.
 * Per-module migration is tracked in `docs/MOCK_REMOVAL_REPORT.md`.
 */
import axios, {
  AxiosError,
  AxiosInstance,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from "axios";
import { tokenStorage } from "@/lib/tokenStorage";
import { getOrCreateRequestId } from "@/lib/requestId";
import { ApiError, type ApiResponse } from "./apiClient";

/** Backend response envelope (mirrors com.commercesuite.common.api.ApiResponse). */
interface BackendEnvelope<T> {
  success: boolean;
  data: T | null;
  message: string | null;
  timestamp: string;
}

const BASE_URL = (import.meta.env.VITE_API_URL ?? "/api/v1").replace(/\/+$/, "");
const TIMEOUT = Number(import.meta.env.VITE_API_TIMEOUT_MS ?? 20000);

const instance: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: TIMEOUT,
  headers: { "Content-Type": "application/json", Accept: "application/json" },
});

/* --------------------------- request interceptor -------------------------- */
instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // JWT injection — opt-out via `headers['X-Skip-Auth'] = '1'`.
  const skipAuth = config.headers?.["X-Skip-Auth"] === "1";
  if (!skipAuth) {
    const token = tokenStorage.getAccess();
    if (token) config.headers.set("Authorization", `Bearer ${token}`);
  }
  config.headers.set("X-Request-Id", getOrCreateRequestId());
  if (config.headers?.["X-Skip-Auth"] !== undefined) {
    config.headers.delete("X-Skip-Auth");
  }
  return config;
});

/* -------------------------- response interceptor -------------------------- */
let refreshInFlight: Promise<string | null> | null = null;

async function performRefresh(): Promise<string | null> {
  if (refreshInFlight) return refreshInFlight;
  const refreshToken = tokenStorage.getRefresh();
  if (!refreshToken) return null;
  refreshInFlight = (async () => {
    try {
      const res = await axios.post<BackendEnvelope<{ accessToken: string; refreshToken: string }>>(
        `${BASE_URL}/auth/refresh`,
        { refreshToken },
        { timeout: TIMEOUT, headers: { "Content-Type": "application/json" } },
      );
      const body = res.data;
      if (!body?.success || !body.data?.accessToken) return null;
      tokenStorage.setAccess(body.data.accessToken);
      tokenStorage.setRefresh(body.data.refreshToken);
      tokenStorage.broadcast("refresh");
      return body.data.accessToken;
    } catch {
      return null;
    } finally {
      refreshInFlight = null;
    }
  })();
  return refreshInFlight;
}

instance.interceptors.response.use(
  (resp) => resp,
  async (err: AxiosError<BackendEnvelope<unknown>>) => {
    const status = err.response?.status;
    const original = err.config as InternalAxiosRequestConfig & { _retried?: boolean };

    // Single retry-after-refresh for protected endpoints.
    if (status === 401 && original && !original._retried && !original.url?.includes("/auth/")) {
      original._retried = true;
      const newToken = await performRefresh();
      if (newToken) {
        original.headers?.set?.("Authorization", `Bearer ${newToken}`);
        return instance(original);
      }
      // Refresh failed — clear and broadcast logout so the app reacts.
      tokenStorage.clear();
      tokenStorage.broadcast("logout");
    }
    return Promise.reject(err);
  },
);

/* ------------------------------- public API ------------------------------- */
function unwrap<T>(envelope: BackendEnvelope<T> | T, status: number): ApiResponse<T> {
  // Accept both true envelopes and bare payloads (defensive — backend always wraps).
  if (envelope && typeof envelope === "object" && "success" in (envelope as object)) {
    const e = envelope as BackendEnvelope<T>;
    if (!e.success) throw new ApiError(e.message ?? "Request failed", status);
    return { data: e.data as T, status, message: e.message ?? "OK" };
  }
  return { data: envelope as T, status, message: "OK" };
}

function toApiError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    const status = err.response?.status ?? 0;
    const body = err.response?.data as BackendEnvelope<unknown> | undefined;
    const message = body?.message ?? err.message ?? "Network error";
    return new ApiError(message, status);
  }
  if (err instanceof ApiError) return err;
  return new ApiError("Unexpected error", 0);
}

export interface HttpRequestOptions extends Omit<AxiosRequestConfig, "url" | "method" | "data" | "params"> {
  skipAuth?: boolean;
}

function withSkipAuth(opts: HttpRequestOptions = {}): AxiosRequestConfig {
  const { skipAuth, headers, ...rest } = opts;
  const merged: AxiosRequestConfig = { ...rest, headers: { ...(headers ?? {}) } };
  if (skipAuth) (merged.headers as Record<string, string>)["X-Skip-Auth"] = "1";
  return merged;
}

export const httpClient = {
  axios: instance,

  async get<T>(path: string, params?: Record<string, unknown>, opts: HttpRequestOptions = {}): Promise<ApiResponse<T>> {
    try {
      const res = await instance.get<BackendEnvelope<T>>(path, { params, ...withSkipAuth(opts) });
      return unwrap<T>(res.data as BackendEnvelope<T>, res.status);
    } catch (err) { throw toApiError(err); }
  },

  async post<T>(path: string, body?: unknown, opts: HttpRequestOptions = {}): Promise<ApiResponse<T>> {
    try {
      const res = await instance.post<BackendEnvelope<T>>(path, body, withSkipAuth(opts));
      return unwrap<T>(res.data as BackendEnvelope<T>, res.status);
    } catch (err) { throw toApiError(err); }
  },

  async put<T>(path: string, body?: unknown, opts: HttpRequestOptions = {}): Promise<ApiResponse<T>> {
    try {
      const res = await instance.put<BackendEnvelope<T>>(path, body, withSkipAuth(opts));
      return unwrap<T>(res.data as BackendEnvelope<T>, res.status);
    } catch (err) { throw toApiError(err); }
  },

  async patch<T>(path: string, body?: unknown, opts: HttpRequestOptions = {}): Promise<ApiResponse<T>> {
    try {
      const res = await instance.patch<BackendEnvelope<T>>(path, body, withSkipAuth(opts));
      return unwrap<T>(res.data as BackendEnvelope<T>, res.status);
    } catch (err) { throw toApiError(err); }
  },

  async delete<T>(path: string, opts: HttpRequestOptions = {}): Promise<ApiResponse<T>> {
    try {
      const res = await instance.delete<BackendEnvelope<T>>(path, withSkipAuth(opts));
      return unwrap<T>(res.data as BackendEnvelope<T>, res.status);
    } catch (err) { throw toApiError(err); }
  },
};

/** Feature flag — true when the app should call the real backend. */
export const USE_REAL_API = import.meta.env.VITE_USE_MOCK_API !== "1";