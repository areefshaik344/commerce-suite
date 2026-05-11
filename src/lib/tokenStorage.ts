/**
 * Token storage abstraction.
 * - Refresh token: localStorage (survives browser restart, swap to httpOnly cookie when backend lands).
 * - Access token: in-memory primarily, sessionStorage fallback for tab reload (NOT localStorage, mitigates XSS exfiltration scope).
 * - All tokens are JWT-shaped (`header.payload.signature`) with base64url-encoded payload that includes `exp` (epoch seconds).
 */

const ACCESS_KEY = "mh.at";
const REFRESH_KEY = "mh.rt";
const PERSIST_KEY = "mh.persist"; // "1" => localStorage, "0" => sessionStorage
const SYNC_KEY = "mh.sync";       // cross-tab broadcast channel via storage event

let memoryAccessToken: string | null = null;
let persistent = true; // default: remember me on

export interface DecodedToken {
  sub: string;
  role: string;
  exp: number; // epoch seconds
  iat: number;
  type: "access" | "refresh";
}

function b64urlEncode(str: string): string {
  return btoa(unescape(encodeURIComponent(str)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function b64urlDecode(str: string): string {
  const pad = str.length % 4 === 0 ? "" : "=".repeat(4 - (str.length % 4));
  const normalized = str.replace(/-/g, "+").replace(/_/g, "/") + pad;
  return decodeURIComponent(escape(atob(normalized)));
}

/** Build a fake JWT — header is fixed, payload encodes claims. Mock only. */
export function signMockToken(payload: Omit<DecodedToken, "iat">): string {
  const header = b64urlEncode(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const fullPayload = { ...payload, iat: Math.floor(Date.now() / 1000) };
  const body = b64urlEncode(JSON.stringify(fullPayload));
  // Deterministic pseudo-signature so refreshing in the mock backend can verify.
  const sig = b64urlEncode(`mock-sig-${fullPayload.sub}-${fullPayload.exp}`);
  return `${header}.${body}.${sig}`;
}

export function decodeToken(token: string | null): DecodedToken | null {
  if (!token) return null;
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  try {
    return JSON.parse(b64urlDecode(parts[1])) as DecodedToken;
  } catch {
    return null;
  }
}

export function isExpired(token: string | null, skewSeconds = 0): boolean {
  const decoded = decodeToken(token);
  if (!decoded) return true;
  return decoded.exp - skewSeconds <= Math.floor(Date.now() / 1000);
}

export const tokenStorage = {
  setPersistent(value: boolean) {
    persistent = value;
    try { localStorage.setItem(PERSIST_KEY, value ? "1" : "0"); } catch { /* ignore */ }
    // If switching to session-only, drop any previously persisted refresh token.
    if (!value) {
      try { localStorage.removeItem(REFRESH_KEY); } catch { /* ignore */ }
    } else {
      try { sessionStorage.removeItem(REFRESH_KEY); } catch { /* ignore */ }
    }
  },
  isPersistent(): boolean {
    try {
      const v = localStorage.getItem(PERSIST_KEY);
      if (v === "0") persistent = false;
      else if (v === "1") persistent = true;
    } catch { /* ignore */ }
    return persistent;
  },
  setAccess(token: string | null) {
    memoryAccessToken = token;
    try {
      if (token) sessionStorage.setItem(ACCESS_KEY, token);
      else sessionStorage.removeItem(ACCESS_KEY);
    } catch { /* private mode */ }
  },
  getAccess(): string | null {
    if (memoryAccessToken) return memoryAccessToken;
    try {
      memoryAccessToken = sessionStorage.getItem(ACCESS_KEY);
    } catch { /* ignore */ }
    return memoryAccessToken;
  },
  setRefresh(token: string | null) {
    const persist = tokenStorage.isPersistent();
    try {
      if (token) {
        if (persist) {
          localStorage.setItem(REFRESH_KEY, token);
          sessionStorage.removeItem(REFRESH_KEY);
        } else {
          sessionStorage.setItem(REFRESH_KEY, token);
          localStorage.removeItem(REFRESH_KEY);
        }
      } else {
        localStorage.removeItem(REFRESH_KEY);
        sessionStorage.removeItem(REFRESH_KEY);
      }
    } catch { /* ignore */ }
  },
  getRefresh(): string | null {
    try {
      return localStorage.getItem(REFRESH_KEY) || sessionStorage.getItem(REFRESH_KEY);
    } catch {
      return null;
    }
  },
  clear() {
    memoryAccessToken = null;
    try {
      sessionStorage.removeItem(ACCESS_KEY);
      sessionStorage.removeItem(REFRESH_KEY);
      localStorage.removeItem(REFRESH_KEY);
    } catch { /* ignore */ }
  },
  /** Broadcast a one-shot auth event to other tabs. */
  broadcast(event: "login" | "logout" | "refresh") {
    try {
      // Set then remove to ensure storage event fires on every call (even with same value).
      localStorage.setItem(SYNC_KEY, `${event}:${Date.now()}`);
      localStorage.removeItem(SYNC_KEY);
    } catch { /* ignore */ }
  },
  SYNC_KEY,
  REFRESH_KEY,
};

export const TOKEN_TTL = {
  ACCESS_SECONDS: 15 * 60,       // 15 min
  REFRESH_SECONDS: 7 * 24 * 3600, // 7 days
  REFRESH_SKEW_SECONDS: 60,       // refresh 60s before expiry
};