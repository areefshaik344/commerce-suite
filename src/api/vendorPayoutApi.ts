/**
 * Vendor payout API — wraps Spring Boot PayoutController
 * (`/api/v1/vendor/payouts`). Money fields are paise; format with
 * `paiseToRupees` at render time.
 */
import { mockSuccess, simulateDelay, type ApiResponse } from "./apiClient";
import { httpClient, USE_REAL_API } from "./httpClient";
import type { BackendPageResponse } from "./orderAdapter";

export type PayoutStatus =
  | "SCHEDULED" | "PENDING" | "PROCESSING" | "PAID" | "FAILED" | "CANCELLED";

export interface VendorPayoutDto {
  id: string; vendorId: string; batchId: string | null; settlementId: string | null;
  status: PayoutStatus; currency: string; amountPaise: number;
  bankReference: string | null; gatewayProvider: string | null;
  failureCode: string | null; failureMessage: string | null;
  scheduledAt: string | null; processedAt: string | null; completedAt: string | null;
}

export const vendorPayoutApi = {
  async list(page = 0, size = 20): Promise<ApiResponse<BackendPageResponse<VendorPayoutDto>>> {
    if (USE_REAL_API) return httpClient.get("/vendor/payouts", { page, size });
    await simulateDelay(200);
    return mockSuccess({ items: [], page, size, total: 0, totalPages: 0 });
  },

  async get(id: string): Promise<ApiResponse<VendorPayoutDto>> {
    if (USE_REAL_API) return httpClient.get(`/vendor/payouts/${id}`);
    await simulateDelay(150);
    throw new Error("Mock payout detail not available");
  },
};