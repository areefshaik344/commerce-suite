import { simulateDelay, mockSuccess, type ApiResponse } from "./apiClient";
import { mockOrderRecords } from "@/mocks/mockOrderRecords";
import type { RefundRecord } from "@/types/order";
import { httpClient, USE_REAL_API } from "./httpClient";
import {
  refundFromBackend, isUuid,
  type BackendRefundRequestDto, type BackendPageResponse,
} from "./orderAdapter";

let DATA = mockOrderRecords;
export function __bindRefundDataset(d: typeof mockOrderRecords) { DATA = d; }

export const refundApi = {
  async listForOrder(orderId: string): Promise<ApiResponse<RefundRecord[]>> {
    if (USE_REAL_API && isUuid(orderId)) {
      // Backend exposes admin-scoped /admin/refunds; filter client-side by orderId.
      const res = await httpClient.get<BackendPageResponse<BackendRefundRequestDto>>(
        "/admin/refunds", { page: 0, size: 100 },
      );
      return mockSuccess(res.data.items.filter(r => r.orderId === orderId).map(refundFromBackend));
    }
    await simulateDelay(150);
    const o = DATA.find(x => x.id === orderId);
    return mockSuccess(o?.refunds ?? []);
  },
  async listForCustomer(customerId: string): Promise<ApiResponse<RefundRecord[]>> {
    await simulateDelay(200);
    const refunds = DATA.filter(o => o.customerId === customerId).flatMap(o => o.refunds);
    return mockSuccess(refunds);
  },
};