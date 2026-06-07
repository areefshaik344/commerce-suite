/**
 * Generates correlation IDs used as `X-Request-Id` on every outbound request
 * and propagated into audit logs, analytics events and webhook payloads.
 */
export function newRequestId(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `req_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 10)}`;
}

export const REQUEST_ID_HEADER = "X-Request-Id";