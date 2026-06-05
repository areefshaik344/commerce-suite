import { create } from "zustand";
import type { Shipment, ShipmentStatus, OrderRecord } from "@/types/order";
import type { ShipmentDetail, TrackingEvent, FulfillmentStatus, ShippingLabel } from "@/types/shipping";
import { shipmentApi } from "@/api/shipmentApi";
import { shippingApi } from "@/api/shippingApi";
import { useOrderStore } from "./orderStore";

interface ShipmentState {
  byId: Record<string, Shipment>;
  detailsById: Record<string, ShipmentDetail>;
  shipmentIdsByOrder: Record<string, string[]>;
  updating: boolean;
  loading: boolean;
  error: string | null;

  fetchById: (id: string) => Promise<Shipment | null>;
  fetchDetail: (id: string) => Promise<ShipmentDetail | null>;
  fetchForOrder: (orderId: string) => Promise<ShipmentDetail[]>;
  trackByNumber: (tracking: string) => Promise<ShipmentDetail | null>;

  updateStatus: (input: {
    orderId: string; shipmentId: string; status: ShipmentStatus;
    trackingNumber?: string; carrier?: string; location?: string; note?: string;
    actorId: string; actorRole: "vendor" | "admin";
  }) => Promise<{ order: OrderRecord; shipment: Shipment }>;

  recordTrackingEvent: (input: {
    shipmentId: string; type: TrackingEvent["type"]; status: FulfillmentStatus;
    location?: string; note?: string; actorId: string; actorRole: "vendor" | "admin";
  }) => Promise<TrackingEvent>;

  generateLabel: (input: { shipmentId: string; courierId: string; actorId: string }) => Promise<ShippingLabel>;
}

export const useShipmentStore = create<ShipmentState>((set, get) => ({
  byId: {},
  detailsById: {},
  shipmentIdsByOrder: {},
  updating: false,
  loading: false,
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

  async fetchDetail(id) {
    set({ loading: true, error: null });
    try {
      const res = await shippingApi.getShipmentDetail(id);
      set(s => ({
        detailsById: { ...s.detailsById, [id]: res.data },
        byId: { ...s.byId, [id]: res.data.shipment },
        loading: false,
      }));
      return res.data;
    } catch (e) {
      set({ loading: false, error: e instanceof Error ? e.message : "Failed to load shipment" });
      return null;
    }
  },

  async fetchForOrder(orderId) {
    set({ loading: true, error: null });
    try {
      const res = await shippingApi.listForOrder(orderId);
      set(s => ({
        detailsById: res.data.reduce((acc, d) => ({ ...acc, [d.shipment.id]: d }), { ...s.detailsById }),
        byId: res.data.reduce((acc, d) => ({ ...acc, [d.shipment.id]: d.shipment }), { ...s.byId }),
        shipmentIdsByOrder: { ...s.shipmentIdsByOrder, [orderId]: res.data.map(d => d.shipment.id) },
        loading: false,
      }));
      return res.data;
    } catch (e) {
      set({ loading: false, error: e instanceof Error ? e.message : "Failed to load shipments" });
      return [];
    }
  },

  async trackByNumber(tracking) {
    set({ loading: true, error: null });
    try {
      const res = await shippingApi.trackByNumber(tracking);
      set(s => ({ detailsById: { ...s.detailsById, [res.data.shipment.id]: res.data }, loading: false }));
      return res.data;
    } catch (e) {
      set({ loading: false, error: e instanceof Error ? e.message : "Tracking not found" });
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

  async recordTrackingEvent(input) {
    const res = await shippingApi.recordTrackingEvent(input);
    // Refresh detail so timeline reflects latest events.
    void get().fetchDetail(input.shipmentId);
    return res.data.event;
  },

  async generateLabel(input) {
    const res = await shippingApi.generateLabel(input);
    void get().fetchDetail(input.shipmentId);
    return res.data;
  },
}));
