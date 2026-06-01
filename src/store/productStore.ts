import { create } from "zustand";
import { productApi, type ProductFilters } from "@/api/productApi";
import type { Product } from "@/data/mock-products";
import type { CatalogProduct } from "@/types/catalog";
import { fromLegacyProduct } from "@/types/catalog";

interface ListingState {
  items: Product[];
  total: number;
  page: number;
  totalPages: number;
  loading: boolean;
  error: string | null;
  filtersKey: string;
}

interface ProductState {
  listing: ListingState;
  currentSlug: string | null;
  current: CatalogProduct | null;
  currentLoading: boolean;
  currentError: string | null;

  fetchList: (filters: ProductFilters) => Promise<void>;
  fetchBySlug: (slug: string) => Promise<void>;
  clearCurrent: () => void;
}

const initialListing: ListingState = {
  items: [],
  total: 0,
  page: 1,
  totalPages: 0,
  loading: false,
  error: null,
  filtersKey: "",
};

function keyOf(f: ProductFilters): string {
  return JSON.stringify(f);
}

export const useProductStore = create<ProductState>((set, get) => ({
  listing: initialListing,
  currentSlug: null,
  current: null,
  currentLoading: false,
  currentError: null,

  fetchList: async (filters) => {
    const filtersKey = keyOf(filters);
    set({ listing: { ...get().listing, loading: true, error: null, filtersKey } });
    try {
      const res = await productApi.getProducts(filters);
      // Guard against stale responses (filters changed while in-flight).
      if (get().listing.filtersKey !== filtersKey) return;
      set({
        listing: {
          items: res.data,
          total: res.total,
          page: res.page,
          totalPages: res.totalPages,
          loading: false,
          error: null,
          filtersKey,
        },
      });
    } catch (e) {
      if (get().listing.filtersKey !== filtersKey) return;
      set({
        listing: {
          ...get().listing,
          loading: false,
          error: e instanceof Error ? e.message : "Failed to load products",
        },
      });
    }
  },

  fetchBySlug: async (slug) => {
    set({ currentSlug: slug, currentLoading: true, currentError: null, current: null });
    try {
      const res = await productApi.getProductBySlug(slug);
      if (get().currentSlug !== slug) return;
      set({
        current: res.data ? fromLegacyProduct(res.data) : null,
        currentLoading: false,
      });
    } catch (e) {
      if (get().currentSlug !== slug) return;
      set({
        currentLoading: false,
        currentError: e instanceof Error ? e.message : "Failed to load product",
      });
    }
  },

  clearCurrent: () => set({ currentSlug: null, current: null, currentError: null }),
}));