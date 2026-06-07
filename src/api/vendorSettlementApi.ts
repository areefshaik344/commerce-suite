/**
 * Vendor settlement API — wraps Spring Boot SettlementController
 * (`/api/v1/vendor/settlements`).
 */
import { mockSuccess, simulateDelay, type ApiResponse } from "./apiClient";
import { httpClient, USE_REAL_API } from "./httpClient";
import type { BackendPageResponse } from "./orderAdapter";

export type SettlementStatus =
  | "DRAFT" | "CALCULATED" | "LOCKED" | "PAID" | "VOID";

export interface SettlementLineDto {
  id: string; vendorOrderId: string;
  grossPaise: number; refundPaise: number;
  commissionPaise: number; platformFeePaise: number; netPaise: number;
}

export interface SettlementDto {
  id: string; vendorId: string; status: SettlementStatus; currency: string;
  periodStart: string; periodEnd: string;
  grossPaise: number; refundPaise: number; commissionPaise: number;
  platformFeePaise: number; adjustmentPaise: number; netPayablePaise: number;
  calculationHash: string | null;
  lockedAt: string | null; paidAt: string | null;
  payoutId: string | null;
  lines: SettlementLineDto[];
}

export const vendorSettlementApi = {
  async list(page = 0, size = 20): Promise<ApiResponse<BackendPageResponse<SettlementDto>>> {
    if (USE_REAL_API) return httpClient.get("/vendor/settlements", { page, size });
    await simulateDelay(200);
    return mockSuccess({ items: [], page, size, total: 0, totalPages: 0 });
  },

  async get(id: string): Promise<ApiResponse<SettlementDto>> {
    if (USE_REAL_API) return httpClient.get(`/vendor/settlements/${id}`);
    await simulateDelay(150);
    throw new Error("Mock settlement detail not available");
  },
};