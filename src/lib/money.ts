/**
 * MONEY_SPEC bridge. Backend stores money as integer paise (BigDecimal-safe).
 * Frontend historically uses `number` (rupees) — these helpers are the ONE
 * place that crosses the boundary so no floating-point drift leaks in.
 */
export function paiseToRupees(paise: number | string | null | undefined): number {
  if (paise == null) return 0;
  const n = typeof paise === "string" ? Number(paise) : paise;
  if (!Number.isFinite(n)) return 0;
  return Math.round(n) / 100;
}

export function rupeesToPaise(rupees: number | string | null | undefined): number {
  if (rupees == null) return 0;
  const n = typeof rupees === "string" ? Number(rupees) : rupees;
  if (!Number.isFinite(n)) return 0;
  // Avoid 0.1 + 0.2 drift: format then strip.
  return Math.round(n * 100);
}

export function formatInr(paise: number | string | null | undefined): string {
  const rupees = paiseToRupees(paise);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  }).format(rupees);
}