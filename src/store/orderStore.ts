import { create } from "zustand";
import type {
  OrderRecord, OrderListFilters, OrderStatus,
} from "@/types/order";
import { orderManagementApi, type ListOrdersParams } from "@/api/orderManagementApi";
import { returnApi } from "@/api/returnApi";

interface OrderState {
  /** Cache keyed by orderId. */
  byId: Record<string, OrderRecord>;
  /** Ordered list of ids per scope key (e.g. `customer:u-1`). */
  listsByScope: Record<string, string[]>;
  totalByScope: Record<string, number>;

  loading: boolean;
  detailLoading: boolean;
  mutating: boolean;
  error: string | null;

  fetchList: (scopeKey: string, params: ListOrdersParams) => Promise<OrderRecord[]>;
  fetchById: (orderId: string) => Promise<OrderRecord | null>;

  cancelItems: (input: { orderId: string; itemIds: string[]; reason: string; note?: string;
    actorId: string; actorRole: "customer"|"vendor"|"admin" }) => Promise<OrderRecord>;

  requestReturn: (input: { orderId: string; itemIds: string[]; reason: string; note?: string;
    pickupAddressId?: string; actorId: string }) => Promise<OrderRecord>;

  updateReturnStatus: (returnId: string, status: Parameters<typeof returnApi.updateStatus>[1],
    actorId: string, actorRole: "vendor"|"admin") => Promise<OrderRecord>;

  upsert: (order: OrderRecord) => void;
  clear: () => void;
}

export const buildScopeKey = (
  scope: "customer" | "vendor" | "admin",
  id?: string,
  filters?: OrderListFilters,
) => {
  const fk = filters?.status?.join(",") ?? "";
  return `${scope}:${id ?? "*"}:${fk}`;
};

export const useOrderStore = create<OrderState>((set, get) => ({
  byId: {},
  listsByScope: {},
  totalByScope: {},
  loading: false,
  detailLoading: false,
  mutating: false,
  error: null,

  async fetchList(scopeKey, params) {
    set({ loading: true, error: null });
    try {
      const res = await orderManagementApi.list(params);
      const items = res.data.items;
      set(state => ({
        byId: items.reduce((acc, o) => ({ ...acc, [o.id]: o }), { ...state.byId }),
        listsByScope: { ...state.listsByScope, [scopeKey]: items.map(i => i.id) },
        totalByScope: { ...state.totalByScope, [scopeKey]: res.data.total },
        loading: false,
      }));
      return items;
    } catch (e) {
      set({ loading: false, error: e instanceof Error ? e.message : "Failed to load orders" });
      return [];
    }
  },

  async fetchById(orderId) {
    set({ detailLoading: true, error: null });
    try {
      const res = await orderManagementApi.getById(orderId);
      set(state => ({ byId: { ...state.byId, [orderId]: res.data }, detailLoading: false }));
      return res.data;
    } catch (e) {
      set({ detailLoading: false, error: e instanceof Error ? e.message : "Failed to load order" });
      return null;
    }
  },

  async cancelItems(input) {
    set({ mutating: true });
    try {
      const res = await orderManagementApi.requestCancellation(input);
      get().upsert(res.data.order);
      return res.data.order;
    } finally {
      set({ mutating: false });
    }
  },

  async requestReturn(input) {
    set({ mutating: true });
    try {
      const res = await returnApi.request(input);
      get().upsert(res.data.order);
      return res.data.order;
    } finally {
      set({ mutating: false });
    }
  },

  async updateReturnStatus(returnId, status, actorId, actorRole) {
    set({ mutating: true });
    try {
      const res = await returnApi.updateStatus(returnId, status, actorId, actorRole);
      get().upsert(res.data);
      return res.data;
    } finally {
      set({ mutating: false });
    }
  },

  upsert(order) {
    set(state => ({ byId: { ...state.byId, [order.id]: order } }));
  },

  clear() {
    set({ byId: {}, listsByScope: {}, totalByScope: {} });
  },
}));