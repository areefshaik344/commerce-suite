import type { ApiResponse } from "./apiClient";
import { orderManagementApi } from "./orderManagementApi";
import type { OrderRecord, ReturnRequest, ReturnStatus } from "@/types/order";

export const returnApi = {
  request: (input: Parameters<typeof orderManagementApi.requestReturn>[0]) =>
    orderManagementApi.requestReturn(input),
  updateStatus: (returnId: string, status: ReturnStatus, actorId: string, actorRole: "vendor"|"admin") =>
    orderManagementApi.updateReturnStatus(returnId, status, actorId, actorRole),
};
export type RequestReturnInput = Parameters<typeof returnApi.request>[0];
export type ApiReturnResponse = ApiResponse<{ order: OrderRecord; returnRequest: ReturnRequest }>;