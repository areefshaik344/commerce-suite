import { simulateDelay, mockSuccess, type ApiResponse } from "./apiClient";
import { mockOrderRecords } from "@/mocks/mockOrderRecords";
import type { RefundRecord } from "@/types/order";
import { httpClient, USE_REAL_API } from "./httpClient";
import {
  refundFromBackend, refundFromStorefront, isUuid,
  type BackendRefundRequestDto, type BackendPageResponse,
  type StorefrontRefundSummaryDto,
} from "./orderAdapter";

let DATA = mockOrderRecords;
export function __bindRefundDataset(d: typeof mockOrderRecords) { DATA = d; }

export const refundApi = {
  async listForOrder(orderId: string): Promise<ApiResponse<RefundRecord[]>> {
    if (USE_REAL_API && isUuid(orderId)) {
      // Storefront customer-scoped refund list (server-side joined to order).
      try {
        const res = await httpClient.get<StorefrontRefundSummaryDto[]>(
          `/storefront/orders/${orderId}/refunds`,
        );
        return mockSuccess(res.data.map(r => refundFromStorefront(r, orderId)));
      } catch {
        // Fallback for admin contexts where storefront scope is denied.
        const res = await httpClient.get<BackendPageResponse<BackendRefundRequestDto>>(
          "/admin/refunds", { page: 0, size: 100 },
        );
        return mockSuccess(res.data.items.filter(r => r.orderId === orderId).map(refundFromBackend));
      }
    }
    await simulateDelay(150);
    const o = DATA.find(x => x.id === orderId);
    return mockSuccess(o?.refunds ?? []);
  },
  async listForCustomer(customerId: string): Promise<ApiResponse<RefundRecord[]>> {
    // No dedicated customer-wide refund endpoint exists yet; the order detail
    // already exposes refunds per order via /storefront/orders/{id}/refunds.
    await simulateDelay(200);
    const refunds = DATA.filter(o => o.customerId === customerId).flatMap(o => o.refunds);
    return mockSuccess(refunds);
  },
};