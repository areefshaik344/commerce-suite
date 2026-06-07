/**
 * Vendor-portal order API — wraps Spring Boot VendorOrderController
 * (`/api/v1/vendor/orders`). Returns raw backend DTOs because the
 * vendor-portal pages still consume their legacy mock shapes for layout;
 * migration of those pages is tracked in
 * `docs/FE_VENDOR_INTEGRATION_REPORT.md` (FE-6 remaining work).
 */
import { mockSuccess, simulateDelay, type ApiResponse } from "./apiClient";
import { httpClient, USE_REAL_API } from "./httpClient";
import type { BackendPageResponse } from "./orderAdapter";

export type VendorOrderStatus =
  | "PENDING_PAYMENT" | "CREATED" | "CONFIRMED" | "PROCESSING" | "PACKED"
  | "SHIPPED" | "OUT_FOR_DELIVERY" | "DELIVERED" | "CANCELLED"
  | "RETURN_REQUESTED" | "RETURNED" | "REFUNDED" | "COMPLETED" | "CLOSED";

export interface VendorOrderItemDto {
  id: string; vendorOrderId: string; productId: string; variantId: string | null;
  sku: string; qty: number;
  unitPricePaise: number; lineTotalPaise: number; status: string;
}

export interface VendorOrderDto {
  id: string; orderId: string; vendorId: string; status: VendorOrderStatus;
  subtotalPaise: number; shippingPaise: number; taxPaise: number; totalPaise: number;
  items: VendorOrderItemDto[];
  createdAt: string; updatedAt: string;
}

export const vendorOrderApi = {
  async list(page = 0, size = 20): Promise<ApiResponse<BackendPageResponse<VendorOrderDto>>> {
    if (USE_REAL_API) return httpClient.get("/vendor/orders", { page, size });
    await simulateDelay(200);
    return mockSuccess({ items: [], page, size, total: 0, totalPages: 0 });
  },

  async get(id: string): Promise<ApiResponse<VendorOrderDto>> {
    if (USE_REAL_API) return httpClient.get(`/vendor/orders/${id}`);
    await simulateDelay(150);
    throw new Error("Mock vendor-order detail not available");
  },

  accept(id: string)  { return httpClient.post<VendorOrderDto>(`/vendor/orders/${id}/accept`); },
  process(id: string) { return httpClient.post<VendorOrderDto>(`/vendor/orders/${id}/process`); },
  ship(id: string)    { return httpClient.post<VendorOrderDto>(`/vendor/orders/${id}/ship`); },
  deliver(id: string) { return httpClient.post<VendorOrderDto>(`/vendor/orders/${id}/deliver`); },
  approveReturn(vendorOrderId: string, returnId: string) {
    return httpClient.post(`/vendor/orders/${vendorOrderId}/returns/${returnId}/approve`);
  },
};