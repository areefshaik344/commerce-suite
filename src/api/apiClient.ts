/**
 * Mock API Client
 * Simulates network requests with configurable delay and error rates.
 * When a real backend is added, swap the implementation here.
 */

export interface ApiResponse<T> {
  data: T;
  status: number;
  message: string;
}

export interface PaginatedResponse<T> {
  data: T[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

export class ApiError extends Error {
  status: number;
  code?: string;
  constructor(message: string, status: number = 500, code?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

const DEFAULT_DELAY = 300; // ms

// Simulate network delay
const delay = (ms: number = DEFAULT_DELAY): Promise<void> =>
  new Promise(resolve => setTimeout(resolve, ms + Math.random() * 200));

/* ---------------------------------------------------------------- *
 *  Production-grade request pipeline (mock transport, real shape)  *
 * ---------------------------------------------------------------- */

export interface RequestConfig {
  endpoint: string;
  method: "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
  body?: unknown;
  params?: Record<string, unknown>;
  /** When true, do NOT inject the Authorization header (used by /auth/* endpoints). */
  skipAuth?: boolean;
  /** When true, do NOT attempt a 401-triggered refresh+retry (used by /auth/refresh). */
  skipAuthRefresh?: boolean;
  /** Abort signal — request is rejected with `AbortError` when triggered. */
  signal?: AbortSignal;
  /** Internal: prevents infinite retry loops. */
  _retried?: boolean;
  /** Optional handler invoked by transport (mock backend hands the work here). */
  handler?: () => Promise<unknown>;
  headers?: Record<string, string>;
}

type RequestInterceptor = (cfg: RequestConfig) => RequestConfig | Promise<RequestConfig>;
type ResponseInterceptor = (res: ApiResponse<unknown>, cfg: RequestConfig) => ApiResponse<unknown> | Promise<ApiResponse<unknown>>;
type ErrorInterceptor = (err: unknown, cfg: RequestConfig) => Promise<ApiResponse<unknown>>;

const requestInterceptors: RequestInterceptor[] = [];
const responseInterceptors: ResponseInterceptor[] = [];
const errorInterceptors: ErrorInterceptor[] = [];

/** Pending in-flight requests keyed by AbortController so logout can cancel them all. */
const pending = new Set<AbortController>();

export function abortAllPending(reason: string = "auth-cancelled") {
  for (const c of pending) {
    try { c.abort(reason); } catch { /* ignore */ }
  }
  pending.clear();
}

function isAbortError(err: unknown): boolean {
  return err instanceof DOMException && err.name === "AbortError";
}

async function waitOrAbort(ms: number, signal?: AbortSignal): Promise<void> {
  if (signal?.aborted) throw new DOMException("Aborted", "AbortError");
  return new Promise((resolve, reject) => {
    const t = setTimeout(() => {
      signal?.removeEventListener("abort", onAbort);
      resolve();
    }, ms + Math.random() * 200);
    function onAbort() {
      clearTimeout(t);
      reject(new DOMException("Aborted", "AbortError"));
    }
    signal?.addEventListener("abort", onAbort, { once: true });
  });
}

export const apiClient = {
  interceptors: {
    request:  { use: (fn: RequestInterceptor)  => { requestInterceptors.push(fn); } },
    response: { use: (fn: ResponseInterceptor) => { responseInterceptors.push(fn); } },
    error:    { use: (fn: ErrorInterceptor)    => { errorInterceptors.push(fn); } },
  },

  /** Single execution path for every request — interceptors, abort, retry-on-401. */
  async request<T>(input: RequestConfig): Promise<ApiResponse<T>> {
    let cfg: RequestConfig = { ...input };
    // Run request interceptors.
    for (const i of requestInterceptors) cfg = await i(cfg);

    // External signal merged with internal controller — supports both.
    const controller = new AbortController();
    if (cfg.signal) {
      if (cfg.signal.aborted) controller.abort(cfg.signal.reason);
      else cfg.signal.addEventListener("abort", () => controller.abort(cfg.signal!.reason), { once: true });
    }
    pending.add(controller);

    try {
      await waitOrAbort(DEFAULT_DELAY, controller.signal);
      if (!cfg.handler) throw new ApiError(`No handler for ${cfg.method} ${cfg.endpoint}`, 404, "NO_HANDLER");
      const raw = await cfg.handler();
      let res = raw as ApiResponse<unknown>;
      for (const i of responseInterceptors) res = await i(res, cfg);
      return res as ApiResponse<T>;
    } catch (err) {
      if (isAbortError(err)) throw err;
      // Walk error interceptors — first one that resolves wins (handles 401→refresh→replay).
      for (const i of errorInterceptors) {
        try {
          const recovered = await i(err, cfg);
          return recovered as ApiResponse<T>;
        } catch (next) {
          err = next;
        }
      }
      throw err;
    } finally {
      pending.delete(controller);
    }
  },

  get<T>(endpoint: string, params?: Record<string, unknown>, opts: Partial<RequestConfig> = {}) {
    return apiClient.request<T>({ endpoint, method: "GET", params, ...opts });
  },
  post<T>(endpoint: string, body?: unknown, opts: Partial<RequestConfig> = {}) {
    return apiClient.request<T>({ endpoint, method: "POST", body, ...opts });
  },
  put<T>(endpoint: string, body?: unknown, opts: Partial<RequestConfig> = {}) {
    return apiClient.request<T>({ endpoint, method: "PUT", body, ...opts });
  },
  delete<T>(endpoint: string, opts: Partial<RequestConfig> = {}) {
    return apiClient.request<T>({ endpoint, method: "DELETE", ...opts });
  },
};

// Helper to create successful mock responses
export function mockSuccess<T>(data: T, message: string = "Success"): ApiResponse<T> {
  return { data, status: 200, message };
}

export function mockPaginated<T>(
  items: T[],
  page: number = 1,
  pageSize: number = 12
): PaginatedResponse<T> {
  const start = (page - 1) * pageSize;
  return {
    data: items.slice(start, start + pageSize),
    total: items.length,
    page,
    pageSize,
    totalPages: Math.ceil(items.length / pageSize),
  };
}

// Simulate network delay helper for direct use
export { delay as simulateDelay };
