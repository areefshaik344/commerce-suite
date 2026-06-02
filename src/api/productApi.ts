import { mockSuccess, mockPaginated, simulateDelay, type ApiResponse, type PaginatedResponse } from "./apiClient";
import { mockProducts, mockCategories } from "@/mocks";
import { mockReviews } from "@/mocks";
import type { Product, Category } from "@/data/mock-products";
import type { Review } from "@/data/mock-orders";
import { PRODUCT_STATUS, type ProductStatus } from "@/types/catalog";
import { assertOwnership, canEditProduct, canSubmitForReview } from "@/lib/productOwnership";

/**
 * Mutable moderation/status overlay keyed by product id. The mock dataset
 * is read-only — overrides let us simulate vendor create/edit and admin
 * moderate without mutating shared fixtures.
 */
const productOverrides: Record<string, { status?: ProductStatus; moderationReason?: string }> = {};
const vendorDrafts: Product[] = [];

function statusOf(id: string): ProductStatus {
  return productOverrides[id]?.status ?? PRODUCT_STATUS.APPROVED;
}

function allProducts(): Product[] {
  return [...vendorDrafts, ...mockProducts];
}

export interface ProductFilters {
  category?: string;
  brand?: string | string[];
  minPrice?: number;
  maxPrice?: number;
  minRating?: number;
  minDiscount?: number;
  inStock?: boolean;
  vendorId?: string;
  status?: ProductStatus;
  search?: string;
  sortBy?: string;
  page?: number;
  pageSize?: number;
  /** attribute filters keyed as `attr.<key>` → value */
  attributes?: Record<string, string>;
}

export const productApi = {
  async getProducts(filters: ProductFilters = {}): Promise<PaginatedResponse<Product>> {
    await simulateDelay(200);
    let result = allProducts().filter((p) =>
      filters.status ? statusOf(p.id) === filters.status : statusOf(p.id) === PRODUCT_STATUS.APPROVED
    );

    if (filters.search) {
      const q = filters.search.toLowerCase();
      result = result.filter(p => p.name.toLowerCase().includes(q) || p.brand.toLowerCase().includes(q) || p.tags.some(t => t.toLowerCase().includes(q)));
    }
    if (filters.category) {
      result = result.filter(p => p.category.toLowerCase().replace(/ & /g, "-").replace(/ /g, "-") === filters.category);
    }
    if (filters.brand) {
      const brands = Array.isArray(filters.brand) ? filters.brand : [filters.brand];
      const set = new Set(brands.map((b) => b.toLowerCase()));
      result = result.filter(p => set.has(p.brand.toLowerCase()));
    }
    if (filters.minPrice !== undefined) {
      result = result.filter(p => p.price >= filters.minPrice!);
    }
    if (filters.maxPrice !== undefined) {
      result = result.filter(p => p.price <= filters.maxPrice!);
    }
    if (filters.minRating !== undefined) {
      result = result.filter(p => p.rating >= filters.minRating!);
    }
    if (filters.minDiscount !== undefined) {
      result = result.filter(p => p.discount >= filters.minDiscount!);
    }
    if (filters.inStock) {
      result = result.filter(p => p.inStock && p.stockCount > 0);
    }
    if (filters.vendorId) {
      result = result.filter(p => p.vendorId === filters.vendorId);
    }
    if (filters.attributes) {
      const entries = Object.entries(filters.attributes);
      result = result.filter(p =>
        entries.every(([k, v]) =>
          Object.entries(p.specs).some(([sk, sv]) => sk.toLowerCase() === k.toLowerCase() && String(sv).toLowerCase() === v.toLowerCase())
        )
      );
    }

    switch (filters.sortBy) {
      case "price-asc": result.sort((a, b) => a.price - b.price); break;
      case "price-desc": result.sort((a, b) => b.price - a.price); break;
      case "rating": result.sort((a, b) => b.rating - a.rating); break;
      case "discount": result.sort((a, b) => b.discount - a.discount); break;
      case "newest": result.sort((a, b) => b.id.localeCompare(a.id)); break;
      case "popularity": result.sort((a, b) => b.reviewCount - a.reviewCount); break;
    }

    return mockPaginated(result, filters.page || 1, filters.pageSize || 12);
  },

  async getProductBySlug(slug: string): Promise<ApiResponse<Product | null>> {
    await simulateDelay(150);
    const product = allProducts().find(p => p.slug === slug) || null;
    return mockSuccess(product);
  },

  async getProductById(id: string): Promise<ApiResponse<Product | null>> {
    await simulateDelay(100);
    return mockSuccess(allProducts().find(p => p.id === id) || null);
  },

  async getCategories(): Promise<ApiResponse<Category[]>> {
    await simulateDelay(100);
    return mockSuccess(mockCategories);
  },

  async getProductReviews(productId: string): Promise<ApiResponse<Review[]>> {
    await simulateDelay(200);
    return mockSuccess(mockReviews.filter(r => r.productId === productId));
  },

  async submitReview(review: Omit<Review, "id" | "helpful">): Promise<ApiResponse<Review>> {
    await simulateDelay(500);
    const newReview: Review = { ...review, id: `r-${Date.now()}`, helpful: 0 };
    mockReviews.push(newReview);
    return mockSuccess(newReview, "Review submitted");
  },

  async getRelatedProducts(productId: string, limit = 4): Promise<ApiResponse<Product[]>> {
    await simulateDelay(150);
    const product = mockProducts.find(p => p.id === productId);
    if (!product) return mockSuccess([]);
    const related = mockProducts.filter(p => p.category === product.category && p.id !== productId).slice(0, limit);
    return mockSuccess(related);
  },

  async getFeaturedProducts(): Promise<ApiResponse<Product[]>> {
    await simulateDelay(100);
    return mockSuccess(mockProducts.filter(p => p.featured));
  },

  async getTrendingProducts(): Promise<ApiResponse<Product[]>> {
    await simulateDelay(100);
    return mockSuccess(mockProducts.filter(p => p.trending));
  },

  async getDeals(): Promise<ApiResponse<Product[]>> {
    await simulateDelay(100);
    return mockSuccess(mockProducts.filter(p => p.discount >= 20));
  },

  async searchSuggestions(query: string): Promise<ApiResponse<string[]>> {
    await simulateDelay(100);
    if (query.length < 2) return mockSuccess([]);
    const q = query.toLowerCase();
    const names = mockProducts
      .filter(p => p.name.toLowerCase().includes(q))
      .slice(0, 6)
      .map(p => p.name);
    return mockSuccess(names);
  },

  async getBrands(): Promise<ApiResponse<string[]>> {
    return mockSuccess([...new Set(mockProducts.map(p => p.brand))]);
  },

  /* --------------- Vendor / Admin operations (mock) --------------- */

  async getVendorProducts(vendorId: string, filters: Omit<ProductFilters, "vendorId"> = {}): Promise<PaginatedResponse<Product>> {
    await simulateDelay(180);
    // For vendor scope we surface every status (incl. drafts they own).
    const owned = allProducts().filter((p) => p.vendorId === vendorId);
    return mockPaginated(owned, filters.page || 1, filters.pageSize || 20);
  },

  async getModerationQueue(filters: { status?: ProductStatus; page?: number; pageSize?: number } = {}): Promise<PaginatedResponse<Product & { status: ProductStatus; moderationReason?: string }>> {
    await simulateDelay(180);
    const status = filters.status ?? PRODUCT_STATUS.PENDING_REVIEW;
    const queue = allProducts()
      .filter((p) => statusOf(p.id) === status)
      .map((p) => ({ ...p, status: statusOf(p.id), moderationReason: productOverrides[p.id]?.moderationReason }));
    return mockPaginated(queue, filters.page || 1, filters.pageSize || 12);
  },

  async createProduct(payload: Omit<Product, "id">, ownerId: string): Promise<ApiResponse<Product>> {
    await simulateDelay(400);
    if (payload.vendorId && payload.vendorId !== ownerId) {
      throw new Error("Cannot create product for another vendor");
    }
    const id = `prod-draft-${Date.now()}`;
    const product: Product = { ...payload, id, vendorId: ownerId };
    vendorDrafts.unshift(product);
    productOverrides[id] = { status: PRODUCT_STATUS.DRAFT };
    return mockSuccess(product, "Product created");
  },

  async updateProduct(id: string, patch: Partial<Product>, ownerId: string): Promise<ApiResponse<Product>> {
    await simulateDelay(300);
    const list = vendorDrafts.some((p) => p.id === id) ? vendorDrafts : mockProducts;
    const idx = list.findIndex((p) => p.id === id);
    if (idx < 0) throw new Error("Product not found");
    if (!canEditProduct({ vendorId: list[idx].vendorId, status: statusOf(id) }, ownerId)) {
      throw new Error("Product is not editable in its current state");
    }
    list[idx] = { ...list[idx], ...patch };
    return mockSuccess(list[idx], "Product updated");
  },

  async archiveProduct(id: string, ownerId: string): Promise<ApiResponse<{ id: string }>> {
    await simulateDelay(250);
    const product = allProducts().find((p) => p.id === id);
    if (!product) throw new Error("Product not found");
    assertOwnership({ vendorId: product.vendorId }, ownerId);
    productOverrides[id] = { ...productOverrides[id], status: PRODUCT_STATUS.ARCHIVED };
    return mockSuccess({ id }, "Product archived");
  },

  async submitForReview(id: string, ownerId: string): Promise<ApiResponse<{ id: string; status: ProductStatus }>> {
    await simulateDelay(250);
    const product = allProducts().find((p) => p.id === id);
    if (!product) throw new Error("Product not found");
    assertOwnership({ vendorId: product.vendorId }, ownerId);
    if (!canSubmitForReview(statusOf(id))) {
      throw new Error("Only DRAFT or REJECTED products can be submitted for review");
    }
    productOverrides[id] = { ...productOverrides[id], status: PRODUCT_STATUS.PENDING_REVIEW };
    return mockSuccess({ id, status: PRODUCT_STATUS.PENDING_REVIEW }, "Submitted for review");
  },

  async moderateProduct(
    id: string,
    action: "approve" | "reject" | "archive" | "feature" | "unfeature",
    reason?: string
  ): Promise<ApiResponse<{ id: string; status: ProductStatus }>> {
    await simulateDelay(300);
    const product = allProducts().find((p) => p.id === id);
    if (!product) throw new Error("Product not found");
    let status: ProductStatus = statusOf(id);
    if (action === "approve") status = PRODUCT_STATUS.APPROVED;
    else if (action === "reject") status = PRODUCT_STATUS.REJECTED;
    else if (action === "archive") status = PRODUCT_STATUS.ARCHIVED;
    productOverrides[id] = { status, moderationReason: action === "reject" ? reason : undefined };
    return mockSuccess({ id, status }, `Product ${action}d`);
  },

  /** Read the current moderation status for a product (used by vendor/admin UI). */
  getStatus(id: string): ProductStatus {
    return statusOf(id);
  },
};
