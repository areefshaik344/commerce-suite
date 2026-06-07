/**
 * Vendor-scoped product CRUD API — wraps Spring Boot ProductController
 * (`/api/v1/products`). Only the vendor-restricted endpoints are exposed
 * here; the public catalog uses `productApi` / `storefrontAdapter`.
 */
import { mockSuccess, simulateDelay, type ApiResponse } from "./apiClient";
import { httpClient, USE_REAL_API } from "./httpClient";
import type { BackendPageResponse } from "./orderAdapter";

export interface BackendProductDto {
  id: string; vendorId: string; categoryId: string; brandId: string | null;
  title: string; slug: string; description: string | null;
  status: "DRAFT" | "PENDING_REVIEW" | "APPROVED" | "REJECTED" | "ARCHIVED";
  basePricePaise: number; salePricePaise: number | null; currency: string;
  primaryImageUrl: string | null;
  createdAt: string; updatedAt: string;
}

export interface CreateProductRequest {
  title: string; slug?: string; description?: string;
  categoryId: string; brandId?: string;
  basePricePaise: number; salePricePaise?: number; currency?: string;
  primaryImageUrl?: string;
}

export type UpdateProductRequest = Partial<CreateProductRequest>;

export interface BackendProductVariantDto {
  id: string; productId: string; sku: string;
  options: Record<string, string>;
  pricePaise: number; salePricePaise: number | null;
  weightGrams: number | null; barcode: string | null; active: boolean;
}

export interface UpsertVariantRequest {
  sku: string; options: Record<string, string>;
  pricePaise: number; salePricePaise?: number;
  weightGrams?: number; barcode?: string; active?: boolean;
}

export const vendorProductApi = {
  async listMine(page = 0, size = 20): Promise<ApiResponse<BackendPageResponse<BackendProductDto>>> {
    if (USE_REAL_API) return httpClient.get("/products/mine", { page, size });
    await simulateDelay(200);
    return mockSuccess({ items: [], page, size, total: 0, totalPages: 0 });
  },
  get(id: string)         { return httpClient.get<BackendProductDto>(`/products/${id}`); },
  create(req: CreateProductRequest) { return httpClient.post<BackendProductDto>("/products", req); },
  update(id: string, req: UpdateProductRequest) { return httpClient.put<BackendProductDto>(`/products/${id}`, req); },
  submit(id: string)      { return httpClient.post<BackendProductDto>(`/products/${id}/submit`); },
  archive(id: string)     { return httpClient.post<BackendProductDto>(`/products/${id}/archive`); },
  listVariants(id: string){ return httpClient.get<BackendProductVariantDto[]>(`/products/${id}/variants`); },
  addVariant(id: string, req: UpsertVariantRequest) {
    return httpClient.post<BackendProductVariantDto>(`/products/${id}/variants`, req);
  },
};