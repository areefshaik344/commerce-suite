import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import type { ProductFilters } from "@/api/productApi";

/**
 * URL-synced product filters with a 300ms debounce.
 *
 * Query contract:
 *   ?page=1&q=phone&category=electronics&brand=Apple,Samsung
 *   &minPrice=1000&maxPrice=50000&rating=4&inStock=1&minDiscount=10
 *   &sort=price-asc&attr.color=Black
 */
const DEBOUNCE_MS = 300;

function parse(params: URLSearchParams): ProductFilters {
  const attrs: Record<string, string> = {};
  for (const [k, v] of params.entries()) if (k.startsWith("attr.")) attrs[k.slice(5)] = v;
  return {
    page: Number(params.get("page") ?? 1) || 1,
    pageSize: Number(params.get("pageSize") ?? 12) || 12,
    search: params.get("q") ?? undefined,
    category: params.get("category") ?? undefined,
    brand: params.get("brand")?.split(",").filter(Boolean),
    minPrice: params.get("minPrice") ? Number(params.get("minPrice")) : undefined,
    maxPrice: params.get("maxPrice") ? Number(params.get("maxPrice")) : undefined,
    minRating: params.get("rating") ? Number(params.get("rating")) : undefined,
    minDiscount: params.get("minDiscount") ? Number(params.get("minDiscount")) : undefined,
    inStock: params.get("inStock") === "1" || undefined,
    vendorId: params.get("vendor") ?? undefined,
    sortBy: params.get("sort") ?? undefined,
    attributes: Object.keys(attrs).length ? attrs : undefined,
  };
}

function serialize(f: ProductFilters): URLSearchParams {
  const p = new URLSearchParams();
  if (f.page && f.page > 1) p.set("page", String(f.page));
  if (f.pageSize && f.pageSize !== 12) p.set("pageSize", String(f.pageSize));
  if (f.search) p.set("q", f.search);
  if (f.category) p.set("category", f.category);
  if (f.brand) {
    const list = Array.isArray(f.brand) ? f.brand : [f.brand];
    if (list.length) p.set("brand", list.join(","));
  }
  if (f.minPrice !== undefined) p.set("minPrice", String(f.minPrice));
  if (f.maxPrice !== undefined) p.set("maxPrice", String(f.maxPrice));
  if (f.minRating !== undefined) p.set("rating", String(f.minRating));
  if (f.minDiscount !== undefined) p.set("minDiscount", String(f.minDiscount));
  if (f.inStock) p.set("inStock", "1");
  if (f.vendorId) p.set("vendor", f.vendorId);
  if (f.sortBy) p.set("sort", f.sortBy);
  if (f.attributes) for (const [k, v] of Object.entries(f.attributes)) p.set(`attr.${k}`, v);
  return p;
}

export function useProductFilters() {
  const [params, setParams] = useSearchParams();
  const [draft, setDraft] = useState<ProductFilters>(() => parse(params));
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Re-parse when URL changes externally (back/forward nav, link clicks).
  useEffect(() => {
    setDraft(parse(params));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params.toString()]);

  const commit = useCallback(
    (next: ProductFilters) => {
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(() => {
        setParams(serialize(next), { replace: true });
      }, DEBOUNCE_MS);
    },
    [setParams]
  );

  const update = useCallback(
    (patch: Partial<ProductFilters> | ((prev: ProductFilters) => ProductFilters)) => {
      setDraft((prev) => {
        const next = typeof patch === "function" ? patch(prev) : { ...prev, ...patch, page: 1 };
        commit(next);
        return next;
      });
    },
    [commit]
  );

  const setPage = useCallback(
    (page: number) => {
      setDraft((prev) => {
        const next = { ...prev, page };
        setParams(serialize(next), { replace: false });
        return next;
      });
    },
    [setParams]
  );

  const clearAll = useCallback(() => {
    setDraft({ page: 1 });
    setParams(new URLSearchParams(), { replace: true });
  }, [setParams]);

  const removeFilter = useCallback(
    (key: keyof ProductFilters | `attr.${string}`) => {
      setDraft((prev) => {
        const next: ProductFilters = { ...prev, page: 1 };
        if (typeof key === "string" && key.startsWith("attr.")) {
          const attrKey = key.slice(5);
          const attrs = { ...(next.attributes ?? {}) };
          delete attrs[attrKey];
          next.attributes = Object.keys(attrs).length ? attrs : undefined;
        } else {
          delete (next as Record<string, unknown>)[key as string];
        }
        commit(next);
        return next;
      });
    },
    [commit]
  );

  const activeChips = useMemo(() => {
    const chips: { key: string; label: string }[] = [];
    if (draft.search) chips.push({ key: "search", label: `"${draft.search}"` });
    if (draft.category) chips.push({ key: "category", label: draft.category });
    if (draft.brand) {
      const list = Array.isArray(draft.brand) ? draft.brand : [draft.brand];
      list.forEach((b) => chips.push({ key: `brand`, label: b }));
    }
    if (draft.minPrice !== undefined || draft.maxPrice !== undefined) {
      chips.push({
        key: "minPrice",
        label: `₹${draft.minPrice ?? 0} - ₹${draft.maxPrice ?? "∞"}`,
      });
    }
    if (draft.minRating !== undefined) chips.push({ key: "minRating", label: `${draft.minRating}★ & up` });
    if (draft.minDiscount !== undefined) chips.push({ key: "minDiscount", label: `${draft.minDiscount}%+ off` });
    if (draft.inStock) chips.push({ key: "inStock", label: "In Stock" });
    if (draft.attributes)
      for (const [k, v] of Object.entries(draft.attributes)) chips.push({ key: `attr.${k}`, label: `${k}: ${v}` });
    return chips;
  }, [draft]);

  return { filters: draft, update, setPage, clearAll, removeFilter, activeChips };
}