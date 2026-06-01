import { useEffect } from "react";
import { useProductStore } from "@/store/productStore";
import type { ProductFilters } from "@/api/productApi";

/**
 * Listing hook — fetches whenever the filter signature changes.
 * Pagination is driven by `filters.page` (URL-synced upstream).
 */
export function useProducts(filters: ProductFilters) {
  const listing = useProductStore((s) => s.listing);
  const fetchList = useProductStore((s) => s.fetchList);

  const key = JSON.stringify(filters);
  useEffect(() => {
    void fetchList(filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key]);

  return {
    products: listing.items,
    total: listing.total,
    page: listing.page,
    totalPages: listing.totalPages,
    loading: listing.loading,
    error: listing.error,
    refetch: () => fetchList(filters),
  };
}

export function useProduct(slug: string | undefined) {
  const current = useProductStore((s) => s.current);
  const loading = useProductStore((s) => s.currentLoading);
  const error = useProductStore((s) => s.currentError);
  const fetchBySlug = useProductStore((s) => s.fetchBySlug);
  const clearCurrent = useProductStore((s) => s.clearCurrent);

  useEffect(() => {
    if (slug) void fetchBySlug(slug);
    return () => clearCurrent();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [slug]);

  return { product: current, loading, error };
}