import { mockSuccess, simulateDelay, type ApiResponse } from "./apiClient";
import { mockCategories } from "@/mocks";
import type { CategoryNode } from "@/types/catalog";
import { httpClient, USE_REAL_API } from "./httpClient";
import { ApiError } from "./apiClient";

/**
 * Category API — nested tree, breadcrumbs, slug lookup.
 * Backed by the existing flat mockCategories list; subcategories are
 * promoted to child CategoryNodes so the tree-rendering UI is real.
 */
function buildTree(): CategoryNode[] {
  return mockCategories.map((c) => ({
    id: c.id,
    name: c.name,
    slug: c.slug,
    parentId: null,
    icon: c.icon,
    productCount: c.productCount,
    children: (c.subcategories ?? []).map((sub, i) => ({
      id: `${c.id}-${i}`,
      name: sub,
      slug: `${c.slug}/${sub.toLowerCase().replace(/\s+/g, "-")}`,
      parentId: c.id,
    })),
  }));
}

function findInTree(tree: CategoryNode[], slug: string): CategoryNode | null {
  for (const n of tree) {
    if (n.slug === slug) return n;
    const child = n.children?.find((c) => c.slug === slug);
    if (child) return child;
  }
  return null;
}

export const categoryApi = {
  async getCategoryTree(): Promise<ApiResponse<CategoryNode[]>> {
    if (USE_REAL_API) {
      try {
        const res = await httpClient.get<BackendCategoryDto[]>("/catalog/categories", undefined, { skipAuth: true });
        return { data: res.data.map(toCategoryNode), status: res.status, message: res.message };
      } catch (err) {
        // Graceful fallback to mocks if the backend is unreachable in this build.
        if (!(err instanceof ApiError)) throw err;
      }
    }
    await simulateDelay(150);
    return mockSuccess(buildTree(), "Categories loaded");
  },

  async getCategoryBySlug(slug: string): Promise<ApiResponse<CategoryNode | null>> {
    await simulateDelay(100);
    return mockSuccess(findInTree(buildTree(), slug));
  },

  async getBreadcrumbs(slug: string): Promise<ApiResponse<CategoryNode[]>> {
    await simulateDelay(80);
    const tree = buildTree();
    const node = findInTree(tree, slug);
    if (!node) return mockSuccess([]);
    if (!node.parentId) return mockSuccess([node]);
    const parent = tree.find((c) => c.id === node.parentId);
    return mockSuccess(parent ? [parent, node] : [node]);
  },
};

/* ------------------------------------------------------------------ *
 *  Real backend adapter (Spring Boot CategoryController)              *
 * ------------------------------------------------------------------ */
interface BackendCategoryDto {
  id: string; parentId: string | null; name: string; slug: string;
  description: string | null; icon: string | null;
  sortOrder: number; active: boolean; children: BackendCategoryDto[];
}

function toCategoryNode(c: BackendCategoryDto): CategoryNode {
  return {
    id: c.id,
    name: c.name,
    slug: c.slug,
    parentId: c.parentId,
    icon: c.icon ?? undefined,
    children: (c.children ?? []).map(toCategoryNode),
  };
}