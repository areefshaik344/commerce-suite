/**
 * Vendor inventory API — wraps Spring Boot InventoryController
 * (`/api/v1/inventory`). Permission-scoped so only the calling vendor's
 * items are returned.
 */
import { mockSuccess, simulateDelay, type ApiResponse } from "./apiClient";
import { httpClient, USE_REAL_API } from "./httpClient";
import type { BackendPageResponse } from "./orderAdapter";

export interface InventoryItemDto {
  id: string; variantId: string; vendorId: string; sku: string;
  available: number; reserved: number; sold: number; safetyStock: number;
  lowStockThreshold: number | null; updatedAt: string;
}

export interface UpdateInventoryRequest {
  available?: number; safetyStock?: number; lowStockThreshold?: number;
}

export interface AdjustInventoryRequest {
  deltaQty: number; reason: string; reference?: string;
}

export interface AdjustmentDto {
  id: string; variantId: string; deltaQty: number; reason: string;
  reference: string | null; createdAt: string;
}

export interface ReserveInventoryRequest {
  qty: number; reference: string; ttlSeconds?: number;
}

export interface ReservationDto {
  id: string; variantId: string; qty: number; reference: string;
  status: "ACTIVE" | "COMMITTED" | "RELEASED" | "EXPIRED";
  expiresAt: string | null; createdAt: string;
}

export interface UpsertLowStockRuleRequest {
  threshold: number; notifyEmail?: boolean;
}

export interface LowStockRuleDto {
  variantId: string; threshold: number; notifyEmail: boolean;
}

export const vendorInventoryApi = {
  async listMine(page = 0, size = 20): Promise<ApiResponse<BackendPageResponse<InventoryItemDto>>> {
    if (USE_REAL_API) return httpClient.get("/inventory", { page, size });
    await simulateDelay(200);
    return mockSuccess({ items: [], page, size, total: 0, totalPages: 0 });
  },
  get(variantId: string)    { return httpClient.get<InventoryItemDto>(`/inventory/${variantId}`); },
  update(variantId: string, req: UpdateInventoryRequest) {
    return httpClient.put<InventoryItemDto>(`/inventory/${variantId}`, req);
  },
  init(variantId: string)   { return httpClient.post<InventoryItemDto>(`/inventory/${variantId}/init`); },
  adjust(variantId: string, req: AdjustInventoryRequest) {
    return httpClient.post<AdjustmentDto>(`/inventory/${variantId}/adjust`, req);
  },
  reserve(variantId: string, req: ReserveInventoryRequest) {
    return httpClient.post<ReservationDto>(`/inventory/${variantId}/reserve`, req);
  },
  commit(reservationId: string) { return httpClient.post<ReservationDto>(`/inventory/reservations/${reservationId}/commit`); },
  release(reservationId: string, reason: string) {
    return httpClient.post<ReservationDto>(`/inventory/reservations/${reservationId}/release`, { reason });
  },
  getReservation(id: string)    { return httpClient.get<ReservationDto>(`/inventory/reservations/${id}`); },
  upsertLowStockRule(variantId: string, req: UpsertLowStockRuleRequest) {
    return httpClient.put<LowStockRuleDto>(`/inventory/${variantId}/low-stock-rule`, req);
  },
  getLowStockRule(variantId: string) {
    return httpClient.get<LowStockRuleDto>(`/inventory/${variantId}/low-stock-rule`);
  },
};