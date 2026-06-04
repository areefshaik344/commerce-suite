/**
 * Order Management API — backend-ready surface for the OrderRecord domain.
 *
 * The legacy `orderApi` (mock-orders) is kept untouched to support pre-existing
 * customer/vendor/admin pages. New work MUST go through this module.
 *
 * Mutations are immutable: every helper returns a NEW OrderRecord; the in-memory
 * dataset is replaced atomically. The shape mirrors the future REST contract.
 */
import { simulateDelay, mockSuccess, ApiError, type ApiResponse, mockPaginated } from "./apiClient";
import { mockOrderRecords } from "@/mocks/mockOrderRecords";
import type {
  OrderRecord, OrderListFilters, OrderListResult, OrderStatus,
  ShipmentStatus, OrderTimelineEvent, CancellationRequest, ReturnRequest, RefundRecord,
} from "@/types/order";
import {
  ORDER_STATUS, SHIPMENT_STATUS, RETURN_STATUS, CANCELLATION_STATUS, PAYMENT_STATUS,
} from "@/types/order";
import { buildOrderFromDraft } from "@/lib/orderFactory";
import { deriveOrderStatusFromShipments, getActiveItemQuantity } from "@/lib/orderSelectors";
import { canTransitionShipment } from "@/lib/orderStatus";
import type { OrderDraft } from "@/types/checkout";

/** Mutable backing store — replaced on every write so React state diffs cleanly. */
let DATA: OrderRecord[] = [...mockOrderRecords];

const seq = (prefix: string) => `${prefix}-${Date.now().toString(36)}-${Math.floor(Math.random()*1e4).toString(36)}`;

function replace(order: OrderRecord) {
  DATA = DATA.map(o => o.id === order.id ? order : o);
}

function appendEvent(order: OrderRecord, ev: Omit<OrderTimelineEvent, "id" | "orderId">): OrderRecord {
  const full: OrderTimelineEvent = { ...ev, id: seq("EV"), orderId: order.id };
  return { ...order, timeline: [...order.timeline, full], updatedAt: ev.at };
}

function recomputeStatus(order: OrderRecord): OrderRecord {
  const derived = deriveOrderStatusFromShipments(order);
  if (derived === order.status) return order;
  return { ...order, status: derived };
}

function applyFilters(items: OrderRecord[], f: OrderListFilters): OrderRecord[] {
  return items.filter(o => {
    if (f.customerId && o.customerId !== f.customerId) return false;
    if (f.vendorId && !o.vendorOrders.some(v => v.vendor.vendorId === f.vendorId)) return false;
    if (f.status && f.status.length && !f.status.includes(o.status)) return false;
    if (f.from && o.placedAt < f.from) return false;
    if (f.to   && o.placedAt > f.to) return false;
    if (f.search) {
      const q = f.search.toLowerCase();
      const hay = [
        o.id,
        ...o.items.map(i => i.product.name),
        ...o.vendorOrders.map(v => v.vendor.vendorName),
      ].join(" ").toLowerCase();
      if (!hay.includes(q)) return false;
    }
    return true;
  });
}

export interface ListOrdersParams extends OrderListFilters {
  page?: number;
  pageSize?: number;
}

/* -------------------------------------------------------------------------- */
/* API                                                                        */
/* -------------------------------------------------------------------------- */

export const orderManagementApi = {
  /** Customer/vendor/admin scoped list — caller must pass appropriate filters. */
  async list(params: ListOrdersParams = {}): Promise<ApiResponse<OrderListResult>> {
    await simulateDelay(250);
    const filtered = applyFilters(DATA, params)
      .sort((a, b) => b.placedAt.localeCompare(a.placedAt));
    const paged = mockPaginated(filtered, params.page ?? 1, params.pageSize ?? 10);
    const result: OrderListResult = {
      items: paged.data,
      total: paged.total,
      page: paged.page,
      pageSize: paged.pageSize,
      totalPages: paged.totalPages,
    };
    return mockSuccess(result);
  },

  async getById(orderId: string): Promise<ApiResponse<OrderRecord>> {
    await simulateDelay(200);
    const o = DATA.find(x => x.id === orderId);
    if (!o) throw new ApiError("Order not found", 404, "ORDER_NOT_FOUND");
    return mockSuccess(o);
  },

  /** Create immutable order from checkout draft. */
  async createFromDraft(draft: OrderDraft, orderId?: string): Promise<ApiResponse<OrderRecord>> {
    await simulateDelay(400);
    const id = orderId ?? `ORD-${Date.now().toString(36).toUpperCase()}`;
    const order = buildOrderFromDraft(draft, id, new Date().toISOString());
    DATA = [order, ...DATA];
    return mockSuccess(order, "Order created");
  },

  /* ------------------------------ Cancellation ----------------------------- */

  async requestCancellation(input: {
    orderId: string; itemIds: string[]; reason: string; note?: string; actorId: string;
    actorRole: "customer" | "vendor" | "admin";
  }): Promise<ApiResponse<{ order: OrderRecord; cancellation: CancellationRequest }>> {
    await simulateDelay(350);
    const order = DATA.find(o => o.id === input.orderId);
    if (!order) throw new ApiError("Order not found", 404, "ORDER_NOT_FOUND");

    const targetIds = new Set(input.itemIds);
    const itemsAfter = order.items.map(it => {
      if (!targetIds.has(it.id)) return it;
      const remaining = getActiveItemQuantity(it);
      if (remaining <= 0) throw new ApiError("Item already resolved", 409, "ITEM_NOT_CANCELLABLE");
      const shipment = it.shipmentId ? order.shipments.find(s => s.id === it.shipmentId) : null;
      if (shipment && shipment.status !== SHIPMENT_STATUS.PACKING &&
          shipment.status !== SHIPMENT_STATUS.READY_TO_SHIP) {
        throw new ApiError("Cannot cancel a shipped item", 409, "ITEM_ALREADY_SHIPPED");
      }
      return { ...it, status: "CANCELLED" as const, cancelledQuantity: it.pricing.quantity };
    });

    const refundAmount = order.items
      .filter(i => targetIds.has(i.id))
      .reduce((a, i) => a + i.pricing.total, 0);

    const cancellation: CancellationRequest = {
      id: seq("CXL"),
      orderId: order.id,
      vendorOrderId: null,
      itemIds: input.itemIds,
      reason: input.reason,
      note: input.note,
      status: CANCELLATION_STATUS.COMPLETED,
      refundAmount,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      resolvedAt: new Date().toISOString(),
      requestedBy: input.actorId,
    };

    const allCancelled = itemsAfter.every(i => i.status !== "ACTIVE");
    let next: OrderRecord = {
      ...order,
      items: itemsAfter,
      cancellations: [...order.cancellations, cancellation],
      payment: refundAmount > 0
        ? { ...order.payment, refundedAmount: order.payment.refundedAmount + refundAmount,
            status: order.payment.refundedAmount + refundAmount >= order.payment.amount
              ? PAYMENT_STATUS.REFUNDED : PAYMENT_STATUS.PARTIALLY_REFUNDED,
            updatedAt: new Date().toISOString() }
        : order.payment,
      status: allCancelled ? ORDER_STATUS.CANCELLED : order.status,
      cancelledAt: allCancelled ? new Date().toISOString() : order.cancelledAt,
      updatedAt: new Date().toISOString(),
    };
    next = appendEvent(next, {
      type: allCancelled ? "CANCELLED" : "CANCELLATION_REQUESTED",
      at: new Date().toISOString(),
      actor: { id: input.actorId, role: input.actorRole },
      message: allCancelled ? "Order cancelled" : `${input.itemIds.length} item(s) cancelled`,
      meta: { reason: input.reason },
    });
    replace(next);
    return mockSuccess({ order: next, cancellation }, "Cancellation processed");
  },

  /* ------------------------------- Returns -------------------------------- */

  async requestReturn(input: {
    orderId: string; itemIds: string[]; reason: string; note?: string;
    pickupAddressId?: string; actorId: string;
  }): Promise<ApiResponse<{ order: OrderRecord; returnRequest: ReturnRequest }>> {
    await simulateDelay(400);
    const order = DATA.find(o => o.id === input.orderId);
    if (!order) throw new ApiError("Order not found", 404, "ORDER_NOT_FOUND");

    const targetIds = new Set(input.itemIds);
    const refundAmount = order.items
      .filter(i => targetIds.has(i.id))
      .reduce((a, i) => a + i.pricing.total, 0);
    const vendorOrderId = order.items.find(i => targetIds.has(i.id))?.vendor.vendorId
      ? (order.vendorOrders.find(v => input.itemIds.some(id => v.itemIds.includes(id)))?.id ?? "")
      : "";

    const returnRequest: ReturnRequest = {
      id: seq("RET"),
      orderId: order.id,
      vendorOrderId,
      itemIds: input.itemIds,
      reason: input.reason,
      note: input.note,
      status: RETURN_STATUS.REQUESTED,
      pickupAddressId: input.pickupAddressId ?? null,
      refundAmount,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      pickedUpAt: null,
      refundedAt: null,
      requestedBy: input.actorId,
    };
    const items = order.items.map(it =>
      targetIds.has(it.id) ? { ...it, status: "RETURN_REQUESTED" as const } : it
    );
    let next: OrderRecord = {
      ...order,
      items,
      returns: [...order.returns, returnRequest],
      updatedAt: new Date().toISOString(),
    };
    next = appendEvent(next, {
      type: "RETURN_REQUESTED",
      at: new Date().toISOString(),
      actor: { id: input.actorId, role: "customer" },
      message: `Return requested for ${input.itemIds.length} item(s)`,
      meta: { reason: input.reason },
    });
    replace(next);
    return mockSuccess({ order: next, returnRequest }, "Return requested");
  },

  async updateReturnStatus(returnId: string, status: ReturnRequest["status"], actorId: string, actorRole: "vendor"|"admin"): Promise<ApiResponse<OrderRecord>> {
    await simulateDelay(300);
    const order = DATA.find(o => o.returns.some(r => r.id === returnId));
    if (!order) throw new ApiError("Return not found", 404, "RETURN_NOT_FOUND");
    const now = new Date().toISOString();
    const returns = order.returns.map(r => r.id === returnId ? {
      ...r, status, updatedAt: now,
      pickedUpAt: status === RETURN_STATUS.PICKED_UP ? now : r.pickedUpAt,
      refundedAt: status === RETURN_STATUS.REFUNDED ? now : r.refundedAt,
    } : r);
    const target = returns.find(r => r.id === returnId)!;
    const targetIds = new Set(target.itemIds);
    const items = order.items.map(it => {
      if (!targetIds.has(it.id)) return it;
      if (status === RETURN_STATUS.REJECTED) return { ...it, status: "ACTIVE" as const };
      if (status === RETURN_STATUS.REFUNDED) {
        return { ...it, status: "REFUNDED" as const, returnedQuantity: it.pricing.quantity, refundedAmount: it.pricing.total };
      }
      if (status === RETURN_STATUS.PICKED_UP) return { ...it, status: "RETURNED" as const, returnedQuantity: it.pricing.quantity };
      return it;
    });
    let next: OrderRecord = { ...order, returns, items, updatedAt: now };
    if (status === RETURN_STATUS.REFUNDED) {
      const refund: RefundRecord = {
        id: seq("REF"),
        orderId: order.id,
        paymentId: order.payment.id,
        sourceType: "RETURN",
        sourceId: returnId,
        amount: target.refundAmount,
        status: "COMPLETED",
        reason: target.reason,
        createdAt: now,
        completedAt: now,
      };
      next = {
        ...next,
        refunds: [...next.refunds, refund],
        payment: { ...next.payment,
          refundedAmount: next.payment.refundedAmount + target.refundAmount,
          status: next.payment.refundedAmount + target.refundAmount >= next.payment.amount
            ? PAYMENT_STATUS.REFUNDED : PAYMENT_STATUS.PARTIALLY_REFUNDED,
          updatedAt: now },
      };
    }
    next = appendEvent(next, {
      type: status === RETURN_STATUS.APPROVED ? "RETURN_APPROVED" :
            status === RETURN_STATUS.REJECTED ? "RETURN_REJECTED" :
            status === RETURN_STATUS.PICKED_UP ? "RETURN_PICKED_UP" :
            status === RETURN_STATUS.REFUNDED ? "REFUND_COMPLETED" : "NOTE",
      at: now, actor: { id: actorId, role: actorRole },
      message: `Return ${status.toLowerCase()}`,
    });
    replace(next);
    return mockSuccess(next);
  },
};

/* Test-only helper to reset in-memory dataset between test cases. */
export function __resetOrderManagementMockData() {
  DATA = [...mockOrderRecords];
}