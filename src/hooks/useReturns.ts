import { useMemo } from "react";
import { useOrderStore } from "@/store/orderStore";
import { useAuth } from "./useAuth";
import { getReturnableItems } from "@/lib/orderSelectors";
import type { OrderRecord, ReturnStatus } from "@/types/order";

export function useReturns(order: OrderRecord | undefined | null) {
  const { user } = useAuth();
  const mutating = useOrderStore(s => s.mutating);
  const requestReturn = useOrderStore(s => s.requestReturn);
  const updateReturnStatus = useOrderStore(s => s.updateReturnStatus);

  const eligibility = useMemo(
    () => order ? getReturnableItems(order) : { eligible: false, itemIds: [] as string[] },
    [order],
  );

  return {
    eligibility,
    mutating,
    requestReturn: (input: { itemIds: string[]; reason: string; note?: string; pickupAddressId?: string }) => {
      if (!order || !user) throw new Error("Order or user missing");
      return requestReturn({ ...input, orderId: order.id, actorId: user.id });
    },
    updateReturnStatus: (returnId: string, status: ReturnStatus, role: "vendor"|"admin") => {
      if (!user) throw new Error("Auth required");
      return updateReturnStatus(returnId, status, user.id, role);
    },
  };
}