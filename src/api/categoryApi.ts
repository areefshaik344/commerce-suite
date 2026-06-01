import { mockSuccess, simulateDelay, type ApiResponse } from "./apiClient";
import { mockCategories } from "@/mocks";
import type { CategoryNode } from "@/types/catalog";

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