import { useEffect, useMemo } from "react";
import { useOrderStore, buildScopeKey } from "@/store/orderStore";
import type { OrderListFilters, OrderRecord } from "@/types/order";

export interface UseOrdersOptions extends OrderListFilters {
  scope: "customer" | "vendor" | "admin";
  scopeId?: string;
  page?: number;
  pageSize?: number;
  enabled?: boolean;
}

export function useOrders(opts: UseOrdersOptions) {
  const { scope, scopeId, enabled = true, ...filters } = opts;

  const scopeKey = useMemo(
    () => buildScopeKey(scope, scopeId, filters),
    [scope, scopeId, filters.status?.join(",")],
  );

  const ids = useOrderStore(s => s.listsByScope[scopeKey]);
  const byId = useOrderStore(s => s.byId);
  const total = useOrderStore(s => s.totalByScope[scopeKey] ?? 0);
  const loading = useOrderStore(s => s.loading);
  const error = useOrderStore(s => s.error);
  const fetchList = useOrderStore(s => s.fetchList);

  useEffect(() => {
    if (!enabled) return;
    const params = {
      ...filters,
      ...(scope === "customer" ? { customerId: scopeId } : {}),
      ...(scope === "vendor" ? { vendorId: scopeId } : {}),
    };
    void fetchList(scopeKey, params);
  }, [scopeKey, enabled]);

  const orders: OrderRecord[] = useMemo(
    () => (ids ?? []).map(id => byId[id]).filter(Boolean),
    [ids, byId],
  );

  return { orders, total, loading, error };
}

export function useOrder(orderId: string | undefined) {
  const order = useOrderStore(s => orderId ? s.byId[orderId] : undefined);
  const loading = useOrderStore(s => s.detailLoading);
  const fetchById = useOrderStore(s => s.fetchById);

  useEffect(() => {
    if (!orderId) return;
    void fetchById(orderId);
  }, [orderId]);

  return { order, loading };
}