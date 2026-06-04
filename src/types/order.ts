/**
 * Order Management Domain — backend-ready DTOs.
 *
 * These types are frozen contracts. Orders are immutable snapshots:
 * product, pricing, vendor and inventory state are captured at placement
 * and MUST NOT be re-derived from live product data.
 *
 * Monetary fields are integer ₹ (rupees). Convert to paise only at the
 * payment-gateway boundary.
 */
import type { Address } from "@/data/mock-users";
import type { PaymentMethodId, ShippingMethodId, PricingBreakdown } from "@/types/checkout";

/* -------------------------------------------------------------------------- */
/* Status enums                                                               */
/* -------------------------------------------------------------------------- */

export const ORDER_STATUS = {
  CREATED: "CREATED",
  CONFIRMED: "CONFIRMED",
  PROCESSING: "PROCESSING",
  PARTIALLY_SHIPPED: "PARTIALLY_SHIPPED",
  SHIPPED: "SHIPPED",
  DELIVERED: "DELIVERED",
  CANCELLED: "CANCELLED",
  RETURNED: "RETURNED",
  REFUNDED: "REFUNDED",
} as const;
export type OrderStatus = typeof ORDER_STATUS[keyof typeof ORDER_STATUS];

export const SHIPMENT_STATUS = {
  PACKING: "PACKING",
  READY_TO_SHIP: "READY_TO_SHIP",
  IN_TRANSIT: "IN_TRANSIT",
  OUT_FOR_DELIVERY: "OUT_FOR_DELIVERY",
  DELIVERED: "DELIVERED",
  FAILED_DELIVERY: "FAILED_DELIVERY",
} as const;
export type ShipmentStatus = typeof SHIPMENT_STATUS[keyof typeof SHIPMENT_STATUS];

export const RETURN_STATUS = {
  REQUESTED: "REQUESTED",
  APPROVED: "APPROVED",
  REJECTED: "REJECTED",
  PICKED_UP: "PICKED_UP",
  REFUNDED: "REFUNDED",
} as const;
export type ReturnStatus = typeof RETURN_STATUS[keyof typeof RETURN_STATUS];

export const CANCELLATION_STATUS = {
  REQUESTED: "REQUESTED",
  APPROVED: "APPROVED",
  REJECTED: "REJECTED",
  COMPLETED: "COMPLETED",
} as const;
export type CancellationStatus = typeof CANCELLATION_STATUS[keyof typeof CANCELLATION_STATUS];

export const PAYMENT_STATUS = {
  PENDING: "PENDING",
  AUTHORIZED: "AUTHORIZED",
  CAPTURED: "CAPTURED",
  FAILED: "FAILED",
  REFUND_PENDING: "REFUND_PENDING",
  REFUNDED: "REFUNDED",
  PARTIALLY_REFUNDED: "PARTIALLY_REFUNDED",
} as const;
export type PaymentStatus = typeof PAYMENT_STATUS[keyof typeof PAYMENT_STATUS];

/* -------------------------------------------------------------------------- */
/* Immutable snapshots                                                        */
/* -------------------------------------------------------------------------- */

export interface ProductSnapshot {
  productId: string;
  variantId: string | null;
  sku: string;
  name: string;
  image: string;
  brand?: string;
  category?: string;
  options?: Record<string, string>;
}

export interface VendorSnapshot {
  vendorId: string;
  vendorName: string;
  vendorSlug?: string;
}

export interface PricingSnapshot {
  unitPrice: number;
  quantity: number;
  subtotal: number;
  discount: number;
  tax: number;
  total: number;
  currency: "INR";
}

/* -------------------------------------------------------------------------- */
/* Order items                                                                */
/* -------------------------------------------------------------------------- */

export type OrderItemStatus =
  | "ACTIVE"
  | "CANCELLED"
  | "RETURN_REQUESTED"
  | "RETURNED"
  | "REFUNDED";

export interface OrderItem {
  id: string;
  product: ProductSnapshot;
  vendor: VendorSnapshot;
  pricing: PricingSnapshot;
  status: OrderItemStatus;
  /** Items can ship across multiple shipments — null until packed. */
  shipmentId: string | null;
  cancelledQuantity: number;
  returnedQuantity: number;
  refundedAmount: number;
}

/* -------------------------------------------------------------------------- */
/* Shipment                                                                   */
/* -------------------------------------------------------------------------- */

export interface ShipmentTimelineEvent {
  id: string;
  at: string;
  status: ShipmentStatus;
  location?: string;
  note?: string;
}

export interface Shipment {
  id: string;
  orderId: string;
  vendorOrderId: string;
  vendorId: string;
  status: ShipmentStatus;
  trackingNumber: string | null;
  carrier: string | null;
  methodId: ShippingMethodId;
  shippingCost: number;
  estimatedDeliveryAt: string | null;
  shippedAt: string | null;
  deliveredAt: string | null;
  itemIds: string[];
  timeline: ShipmentTimelineEvent[];
  createdAt: string;
  updatedAt: string;
}

/* -------------------------------------------------------------------------- */
/* Vendor sub-order                                                           */
/* -------------------------------------------------------------------------- */

export interface VendorOrder {
  id: string;
  orderId: string;
  vendor: VendorSnapshot;
  itemIds: string[];
  shipmentIds: string[];
  status: OrderStatus;
  subtotal: number;
  discount: number;
  shipping: number;
  tax: number;
  total: number;
}

/* -------------------------------------------------------------------------- */
/* Payment record                                                             */
/* -------------------------------------------------------------------------- */

export interface PaymentRecord {
  id: string;
  orderId: string;
  methodId: PaymentMethodId;
  status: PaymentStatus;
  amount: number;
  refundedAmount: number;
  /** Opaque gateway reference — null for COD. */
  gatewayRef: string | null;
  capturedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/* -------------------------------------------------------------------------- */
/* Cancellation / Return / Refund                                             */
/* -------------------------------------------------------------------------- */

export interface CancellationRequest {
  id: string;
  orderId: string;
  vendorOrderId: string | null;
  itemIds: string[];
  reason: string;
  note?: string;
  status: CancellationStatus;
  refundAmount: number;
  createdAt: string;
  updatedAt: string;
  resolvedAt: string | null;
  requestedBy: string;
}

export interface ReturnRequest {
  id: string;
  orderId: string;
  vendorOrderId: string;
  itemIds: string[];
  reason: string;
  note?: string;
  status: ReturnStatus;
  pickupAddressId: string | null;
  refundAmount: number;
  createdAt: string;
  updatedAt: string;
  pickedUpAt: string | null;
  refundedAt: string | null;
  requestedBy: string;
}

export interface RefundRecord {
  id: string;
  orderId: string;
  paymentId: string;
  sourceType: "CANCELLATION" | "RETURN" | "ADJUSTMENT";
  sourceId: string;
  amount: number;
  status: "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
  reason: string;
  createdAt: string;
  completedAt: string | null;
}

/* -------------------------------------------------------------------------- */
/* Timeline                                                                   */
/* -------------------------------------------------------------------------- */

export type OrderTimelineEventType =
  | "ORDER_PLACED"
  | "ORDER_CONFIRMED"
  | "PROCESSING"
  | "SHIPMENT_CREATED"
  | "SHIPMENT_DISPATCHED"
  | "SHIPMENT_OUT_FOR_DELIVERY"
  | "SHIPMENT_DELIVERED"
  | "SHIPMENT_FAILED"
  | "CANCELLATION_REQUESTED"
  | "CANCELLED"
  | "RETURN_REQUESTED"
  | "RETURN_APPROVED"
  | "RETURN_REJECTED"
  | "RETURN_PICKED_UP"
  | "REFUND_INITIATED"
  | "REFUND_COMPLETED"
  | "NOTE";

export interface OrderTimelineEvent {
  id: string;
  orderId: string;
  type: OrderTimelineEventType;
  at: string;
  actor: { id: string; role: "customer" | "vendor" | "admin" | "system"; name?: string };
  message: string;
  meta?: Record<string, unknown>;
}

/* -------------------------------------------------------------------------- */
/* Order root                                                                 */
/* -------------------------------------------------------------------------- */

export interface OrderShippingAddressSnapshot extends Address {
  capturedAt: string;
}

export interface OrderRecord {
  id: string;
  customerId: string;
  status: OrderStatus;

  items: OrderItem[];
  vendorOrders: VendorOrder[];
  shipments: Shipment[];
  timeline: OrderTimelineEvent[];

  payment: PaymentRecord;
  refunds: RefundRecord[];
  cancellations: CancellationRequest[];
  returns: ReturnRequest[];

  shippingAddress: OrderShippingAddressSnapshot;
  pricing: PricingBreakdown;

  /** Reservation consumed when order was placed. */
  reservationId: string | null;

  placedAt: string;
  createdAt: string;
  updatedAt: string;
  cancelledAt: string | null;
  deliveredAt: string | null;
}

/* -------------------------------------------------------------------------- */
/* Notification event contracts (forward-compat)                              */
/* -------------------------------------------------------------------------- */

export type OrderNotificationType =
  | "order.placed"
  | "order.confirmed"
  | "order.shipped"
  | "order.delivered"
  | "order.cancelled"
  | "order.refunded"
  | "return.requested"
  | "return.approved";

export interface OrderNotificationEvent {
  type: OrderNotificationType;
  orderId: string;
  vendorOrderId?: string;
  shipmentId?: string;
  at: string;
  payload: Record<string, unknown>;
}

/* -------------------------------------------------------------------------- */
/* List/query                                                                 */
/* -------------------------------------------------------------------------- */

export interface OrderListFilters {
  status?: OrderStatus[];
  vendorId?: string;
  customerId?: string;
  from?: string;
  to?: string;
  search?: string;
}

export interface OrderListResult {
  items: OrderRecord[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
}

/* -------------------------------------------------------------------------- */
/* Reason codes                                                               */
/* -------------------------------------------------------------------------- */

export const CANCELLATION_REASONS = [
  "Ordered by mistake",
  "Found cheaper elsewhere",
  "Delivery taking too long",
  "Payment issue",
  "Other",
] as const;
export type CancellationReason = typeof CANCELLATION_REASONS[number];

export const RETURN_REASONS = [
  "Damaged on arrival",
  "Wrong item delivered",
  "Item not as described",
  "Quality issue",
  "Doesn't fit / size issue",
  "No longer needed",
  "Other",
] as const;
export type ReturnReason = typeof RETURN_REASONS[number];

/* -------------------------------------------------------------------------- */
/* Type guards                                                                */
/* -------------------------------------------------------------------------- */

export function isTerminalOrderStatus(s: OrderStatus): boolean {
  return s === ORDER_STATUS.DELIVERED || s === ORDER_STATUS.CANCELLED ||
         s === ORDER_STATUS.RETURNED || s === ORDER_STATUS.REFUNDED;
}

export function isShippedShipment(s: ShipmentStatus): boolean {
  return s === SHIPMENT_STATUS.IN_TRANSIT ||
         s === SHIPMENT_STATUS.OUT_FOR_DELIVERY ||
         s === SHIPMENT_STATUS.DELIVERED;
}