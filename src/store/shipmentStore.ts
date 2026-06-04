import { create } from "zustand";
import type { Shipment, ShipmentStatus, OrderRecord } from "@/types/order";
import { shipmentApi } from "@/api/shipmentApi";
import { useOrderStore } from "./orderStore";

interface ShipmentState {
  byId: Record<string, Shipment>;
  updating: boolean;
  error: string | null;

  fetchById: (id: string) => Promise<Shipment | null>;
  updateStatus: (input: {
    orderId: string; shipmentId: string; status: ShipmentStatus;
    trackingNumber?: string; carrier?: string; location?: string; note?: string;
    actorId: string; actorRole: "vendor" | "admin";
  }) => Promise<{ order: OrderRecord; shipment: Shipment }>;
}

export const useShipmentStore = create<ShipmentState>((set) => ({
  byId: {},
  updating: false,
  error: null,

  async fetchById(id) {
    try {
      const res = await shipmentApi.getById(id);
      set(s => ({ byId: { ...s.byId, [id]: res.data } }));
      return res.data;
    } catch (e) {
      set({ error: e instanceof Error ? e.message : "Failed to load shipment" });
      return null;
    }
  },

  async updateStatus(input) {
    set({ updating: true, error: null });
    try {
      const res = await shipmentApi.updateStatus(input);
      set(s => ({ byId: { ...s.byId, [res.data.shipment.id]: res.data.shipment }, updating: false }));
      useOrderStore.getState().upsert(res.data.order);
      return res.data;
    } catch (e) {
      set({ updating: false, error: e instanceof Error ? e.message : "Failed to update shipment" });
      throw e;
    }
  },
}));