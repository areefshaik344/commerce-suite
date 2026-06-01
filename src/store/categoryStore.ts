import { create } from "zustand";
import { categoryApi } from "@/api/categoryApi";
import type { CategoryNode } from "@/types/catalog";

interface CategoryState {
  tree: CategoryNode[];
  flat: Record<string, CategoryNode>;
  loading: boolean;
  error: string | null;
  loaded: boolean;
  bootstrap: () => Promise<void>;
  getBySlug: (slug: string) => CategoryNode | undefined;
}

function flatten(tree: CategoryNode[]): Record<string, CategoryNode> {
  const out: Record<string, CategoryNode> = {};
  const walk = (nodes: CategoryNode[]) => {
    for (const n of nodes) {
      out[n.slug] = n;
      if (n.children?.length) walk(n.children);
    }
  };
  walk(tree);
  return out;
}

export const useCategoryStore = create<CategoryState>((set, get) => ({
  tree: [],
  flat: {},
  loading: false,
  error: null,
  loaded: false,
  bootstrap: async () => {
    if (get().loaded || get().loading) return;
    set({ loading: true, error: null });
    try {
      const res = await categoryApi.getCategoryTree();
      set({ tree: res.data, flat: flatten(res.data), loaded: true, loading: false });
    } catch (e) {
      set({ loading: false, error: e instanceof Error ? e.message : "Failed to load categories" });
    }
  },
  getBySlug: (slug) => get().flat[slug],
}));