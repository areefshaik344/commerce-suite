/**
 * Adapter: Spring Boot Orders / Shipments / Returns / Refunds DTOs ↔ frontend domain.
 *
 * The backend DTOs (com.commercesuite.{orders,shipping,returns,refunds}.dto.*) carry
 * only IDs and integer-paise money. The frontend `OrderRecord` domain is richer
 * (vendor/product/address snapshots, timeline, etc.). Where the backend has no
 * source field we fall back to safe placeholders and document the gap in
 * `docs/FE_ORDERS_INTEGRATION_REPORT.md` — these are picked up by a future
 * read-model phase (BE-RM-2) similar to BE-RM-1.
 */
import { paiseToRupees } from "@/lib/money";
import type {
  OrderRecord, OrderStatus, OrderItem, Shipment, VendorOrder,
  ShipmentStatus, ReturnRequest, ReturnStatus, RefundRecord,
  PaymentRecord, PaymentStatus, OrderShippingAddressSnapshot, OrderTimelineEvent,
} from "@/types/order";
import {
  ORDER_STATUS, SHIPMENT_STATUS, RETURN_STATUS, PAYMENT_STATUS,
} from "@/types/order";
import type { PricingBreakdown, PaymentMethodId, ShippingMethodId } from "@/types/checkout";

/* --------------------------- backend DTO shapes --------------------------- */

export type BackendOrderStatus =
  | "PENDING_PAYMENT" | "CREATED" | "CONFIRMED" | "PROCESSING"
  | "PARTIALLY_SHIPPED" | "SHIPPED" | "PARTIALLY_DELIVERED" | "DELIVERED"
  | "PARTIALLY_CANCELLED" | "CANCELLED" | "PARTIALLY_RETURNED" | "RETURNED"
  | "COMPLETED" | "CLOSED";

export type BackendVendorOrderStatus =
  | "PENDING_PAYMENT" | "CREATED" | "CONFIRMED" | "PROCESSING" | "PACKED"
  | "SHIPPED" | "OUT_FOR_DELIVERY" | "DELIVERED" | "CANCELLED"
  | "RETURN_REQUESTED" | "RETURNED" | "REFUNDED" | "COMPLETED" | "CLOSED";

export type BackendShipmentStatus =
  | "CREATED" | "READY_FOR_PICKUP" | "IN_TRANSIT" | "OUT_FOR_DELIVERY"
  | "DELIVERED" | "FAILED" | "RETURN_TO_ORIGIN";

export type BackendReturnStatus =
  | "REQUESTED" | "APPROVED" | "REJECTED" | "RECEIVED" | "COMPLETED";

export type BackendRefundStatus =
  | "PENDING" | "APPROVED" | "PROCESSING" | "COMPLETED" | "REJECTED";

export interface BackendOrderItemDto {
  id: string; vendorId: string; productId: string; variantId: string | null;
  sku: string; qty: number;
  unitPricePaise: number; lineSubtotalPaise: number; lineDiscountPaise: number;
  lineTaxPaise: number; lineTotalPaise: number;
  cancelledQty: number; returnedQty: number; refundedPaise: number;
  status: string; shipmentId: string | null;
}

export interface BackendVendorOrderDto {
  id: string; orderId: string; vendorId: string; status: BackendVendorOrderStatus;
  subtotalPaise: number; discountPaise: number; shippingPaise: number;
  taxPaise: number; totalPaise: number; items: BackendOrderItemDto[];
}

export interface BackendOrderDto {
  id: string; customerId: string; status: BackendOrderStatus; currency: string;
  subtotalPaise: number; discountPaise: number; couponDiscountPaise: number;
  shippingPaise: number; taxPaise: number; platformFeePaise: number;
  grandTotalPaise: number; couponCode: string | null;
  placedAt: string; cancelledAt: string | null; deliveredAt: string | null;
  vendorOrders: BackendVendorOrderDto[];
}

export interface BackendShipmentItemDto { id: string; orderItemId: string; qty: number; }
export interface BackendShipmentDto {
  id: string; orderId: string; vendorOrderId: string; vendorId: string;
  status: BackendShipmentStatus; carrier: string | null; trackingNumber: string | null;
  shippingMethod: string | null; shippingPaise: number;
  shippedAt: string | null; deliveredAt: string | null; estimatedDeliveryAt: string | null;
  items: BackendShipmentItemDto[];
}
export interface BackendTrackingEventDto {
  id: string; shipmentId: string; eventType: string; description: string | null;
  location: string | null; occurredAt: string;
}

export interface BackendReturnItemDto { id: string; orderItemId: string; qty: number; refundPaise: number; }
export interface BackendReturnRequestDto {
  id: string; orderId: string; vendorOrderId: string; vendorId: string; customerId: string;
  status: BackendReturnStatus; reason: string; note: string | null;
  pickupAddressId: string | null; refundPaise: number;
  requestedAt: string; resolvedAt: string | null; receivedAt: string | null;
  items: BackendReturnItemDto[];
}

export interface BackendRefundItemDto { id: string; orderItemId: string; amountPaise: number; }
export interface BackendRefundRequestDto {
  id: string; orderId: string; vendorOrderId: string;
  sourceType: "CANCELLATION" | "RETURN" | "ADJUSTMENT";
  sourceId: string; amountPaise: number; status: BackendRefundStatus; reason: string;
  requestedAt: string; completedAt: string | null; items?: BackendRefundItemDto[];
}

export interface BackendPageResponse<T> {
  items: T[]; page: number; size: number; total: number; totalPages: number;
}

/* ------------------------------ enum bridges ------------------------------ */

export function mapOrderStatus(s: BackendOrderStatus): OrderStatus {
  switch (s) {
    case "PENDING_PAYMENT":
    case "CREATED": return ORDER_STATUS.CREATED;
    case "CONFIRMED": return ORDER_STATUS.CONFIRMED;
    case "PROCESSING": return ORDER_STATUS.PROCESSING;
    case "PARTIALLY_SHIPPED": return ORDER_STATUS.PARTIALLY_SHIPPED;
    case "SHIPPED": return ORDER_STATUS.SHIPPED;
    case "PARTIALLY_DELIVERED":
    case "DELIVERED":
    case "COMPLETED":
    case "CLOSED": return ORDER_STATUS.DELIVERED;
    case "PARTIALLY_CANCELLED":
    case "CANCELLED": return ORDER_STATUS.CANCELLED;
    case "PARTIALLY_RETURNED":
    case "RETURNED": return ORDER_STATUS.RETURNED;
    default: return ORDER_STATUS.CREATED;
  }
}

export function mapShipmentStatus(s: BackendShipmentStatus): ShipmentStatus {
  switch (s) {
    case "CREATED": return SHIPMENT_STATUS.PACKING;
    case "READY_FOR_PICKUP": return SHIPMENT_STATUS.READY_TO_SHIP;
    case "IN_TRANSIT": return SHIPMENT_STATUS.IN_TRANSIT;
    case "OUT_FOR_DELIVERY": return SHIPMENT_STATUS.OUT_FOR_DELIVERY;
    case "DELIVERED": return SHIPMENT_STATUS.DELIVERED;
    case "FAILED":
    case "RETURN_TO_ORIGIN": return SHIPMENT_STATUS.FAILED_DELIVERY;
    default: return SHIPMENT_STATUS.PACKING;
  }
}

export function toBackendShipmentStatus(s: ShipmentStatus): BackendShipmentStatus {
  switch (s) {
    case SHIPMENT_STATUS.PACKING: return "CREATED";
    case SHIPMENT_STATUS.READY_TO_SHIP: return "READY_FOR_PICKUP";
    case SHIPMENT_STATUS.IN_TRANSIT: return "IN_TRANSIT";
    case SHIPMENT_STATUS.OUT_FOR_DELIVERY: return "OUT_FOR_DELIVERY";
    case SHIPMENT_STATUS.DELIVERED: return "DELIVERED";
    case SHIPMENT_STATUS.FAILED_DELIVERY: return "FAILED";
  }
}

export function mapReturnStatus(s: BackendReturnStatus): ReturnStatus {
  switch (s) {
    case "REQUESTED": return RETURN_STATUS.REQUESTED;
    case "APPROVED": return RETURN_STATUS.APPROVED;
    case "REJECTED": return RETURN_STATUS.REJECTED;
    case "RECEIVED": return RETURN_STATUS.PICKED_UP;
    case "COMPLETED": return RETURN_STATUS.REFUNDED;
  }
}

export function mapRefundStatus(s: BackendRefundStatus): RefundRecord["status"] {
  switch (s) {
    case "PENDING":
    case "APPROVED": return "PENDING";
    case "PROCESSING": return "PROCESSING";
    case "COMPLETED": return "COMPLETED";
    case "REJECTED": return "FAILED";
  }
}

/* -------------------------------- mappers -------------------------------- */

const EMPTY_ADDRESS: OrderShippingAddressSnapshot = {
  id: "",
  label: "Shipping",
  name: "",
  phone: "",
  line1: "",
  line2: "",
  city: "",
  state: "",
  pincode: "",
  isDefault: false,
  type: "HOME",
  capturedAt: new Date(0).toISOString(),
};

function itemFromBackend(it: BackendOrderItemDto, vendorId: string): OrderItem {
  const unit = paiseToRupees(it.unitPricePaise);
  return {
    id: it.id,
    product: {
      productId: it.productId,
      variantId: it.variantId,
      sku: it.sku,
      name: it.sku ?? "Product",
      image: "/placeholder.svg",
    },
    vendor: { vendorId, vendorName: "Vendor" },
    pricing: {
      unitPrice: unit,
      quantity: it.qty,
      subtotal: paiseToRupees(it.lineSubtotalPaise),
      discount: paiseToRupees(it.lineDiscountPaise),
      tax: paiseToRupees(it.lineTaxPaise),
      total: paiseToRupees(it.lineTotalPaise),
      currency: "INR",
    },
    status: (it.status as OrderItem["status"]) ?? "ACTIVE",
    shipmentId: it.shipmentId,
    cancelledQuantity: it.cancelledQty,
    returnedQuantity: it.returnedQty,
    refundedAmount: paiseToRupees(it.refundedPaise),
  };
}

function vendorOrderFromBackend(v: BackendVendorOrderDto): VendorOrder {
  return {
    id: v.id,
    orderId: v.orderId,
    vendor: { vendorId: v.vendorId, vendorName: "Vendor" },
    itemIds: v.items.map(i => i.id),
    shipmentIds: [],
    status: mapOrderStatus(v.status as unknown as BackendOrderStatus),
    subtotal: paiseToRupees(v.subtotalPaise),
    discount: paiseToRupees(v.discountPaise),
    shipping: paiseToRupees(v.shippingPaise),
    tax: paiseToRupees(v.taxPaise),
    total: paiseToRupees(v.totalPaise),
  };
}

export function orderFromBackend(o: BackendOrderDto): OrderRecord {
  const items: OrderItem[] = o.vendorOrders.flatMap(v => v.items.map(i => itemFromBackend(i, v.vendorId)));
  const vendors = o.vendorOrders.map(vendorOrderFromBackend);
  const pricing: PricingBreakdown = {
    subtotal: paiseToRupees(o.subtotalPaise),
    discount: paiseToRupees(o.discountPaise + o.couponDiscountPaise),
    shipping: paiseToRupees(o.shippingPaise),
    tax: paiseToRupees(o.taxPaise),
    platformFee: paiseToRupees(o.platformFeePaise),
    grandTotal: paiseToRupees(o.grandTotalPaise),
    currency: "INR",
    appliedCoupons: o.couponCode
      ? [{ code: o.couponCode, discount: paiseToRupees(o.couponDiscountPaise) } as never]
      : [],
    computedAt: Date.parse(o.placedAt) || Date.now(),
    vendorBreakdowns: vendors.map(v => ({
      vendorId: v.vendor.vendorId,
      vendorName: v.vendor.vendorName,
      subtotal: v.subtotal, discount: v.discount, shipping: v.shipping,
      tax: v.tax, total: v.total,
      itemCount: v.itemIds.length,
    })),
  };

  const payment: PaymentRecord = {
    id: `PAY-${o.id}`,
    orderId: o.id,
    methodId: "card" as PaymentMethodId,
    status: PAYMENT_STATUS.CAPTURED as PaymentStatus,
    amount: paiseToRupees(o.grandTotalPaise),
    refundedAmount: 0,
    gatewayRef: null,
    capturedAt: o.placedAt,
    createdAt: o.placedAt,
    updatedAt: o.placedAt,
  };

  const placedEvt: OrderTimelineEvent = {
    id: `EV-${o.id}-placed`,
    orderId: o.id,
    type: "ORDER_PLACED",
    at: o.placedAt,
    actor: { id: o.customerId, role: "customer" },
    message: "Order placed",
  };

  return {
    id: o.id,
    customerId: o.customerId,
    status: mapOrderStatus(o.status),
    items,
    vendorOrders: vendors,
    shipments: [],
    timeline: [placedEvt],
    payment,
    refunds: [],
    cancellations: [],
    returns: [],
    shippingAddress: EMPTY_ADDRESS,
    pricing,
    reservationId: null,
    placedAt: o.placedAt,
    createdAt: o.placedAt,
    updatedAt: o.placedAt,
    cancelledAt: o.cancelledAt,
    deliveredAt: o.deliveredAt,
  };
}

export function shipmentFromBackend(s: BackendShipmentDto): Shipment {
  return {
    id: s.id,
    orderId: s.orderId,
    vendorOrderId: s.vendorOrderId,
    vendorId: s.vendorId,
    status: mapShipmentStatus(s.status),
    trackingNumber: s.trackingNumber,
    carrier: s.carrier,
    methodId: ((s.shippingMethod?.toLowerCase() as ShippingMethodId) ?? "standard"),
    shippingCost: paiseToRupees(s.shippingPaise),
    estimatedDeliveryAt: s.estimatedDeliveryAt,
    shippedAt: s.shippedAt,
    deliveredAt: s.deliveredAt,
    itemIds: s.items.map(i => i.orderItemId),
    timeline: [],
    createdAt: s.shippedAt ?? new Date().toISOString(),
    updatedAt: s.deliveredAt ?? s.shippedAt ?? new Date().toISOString(),
  };
}

export function returnFromBackend(r: BackendReturnRequestDto): ReturnRequest {
  return {
    id: r.id,
    orderId: r.orderId,
    vendorOrderId: r.vendorOrderId,
    itemIds: r.items.map(i => i.orderItemId),
    reason: r.reason,
    note: r.note ?? undefined,
    status: mapReturnStatus(r.status),
    pickupAddressId: r.pickupAddressId,
    refundAmount: paiseToRupees(r.refundPaise),
    createdAt: r.requestedAt,
    updatedAt: r.resolvedAt ?? r.requestedAt,
    pickedUpAt: r.receivedAt,
    refundedAt: r.status === "COMPLETED" ? r.resolvedAt : null,
    requestedBy: r.customerId,
  };
}

export function refundFromBackend(r: BackendRefundRequestDto): RefundRecord {
  return {
    id: r.id,
    orderId: r.orderId,
    paymentId: `PAY-${r.orderId}`,
    sourceType: r.sourceType,
    sourceId: r.sourceId,
    amount: paiseToRupees(r.amountPaise),
    status: mapRefundStatus(r.status),
    reason: r.reason,
    createdAt: r.requestedAt,
    completedAt: r.completedAt,
  };
}

/* ----------------------------- helpers ----------------------------- */

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export const isUuid = (s: string | null | undefined): s is string => !!s && UUID_RE.test(s);

export function idempotencyKey(prefix: string): string {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}