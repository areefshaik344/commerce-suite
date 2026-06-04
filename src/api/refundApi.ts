import { simulateDelay, mockSuccess, type ApiResponse } from "./apiClient";
import { mockOrderRecords } from "@/mocks/mockOrderRecords";
import type { RefundRecord } from "@/types/order";

let DATA = mockOrderRecords;
export function __bindRefundDataset(d: typeof mockOrderRecords) { DATA = d; }

export const refundApi = {
  async listForOrder(orderId: string): Promise<ApiResponse<RefundRecord[]>> {
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