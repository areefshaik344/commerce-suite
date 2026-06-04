import {
  ORDER_STATUS,
  SHIPMENT_STATUS,
  RETURN_STATUS,
  PAYMENT_STATUS,
  type OrderStatus,
  type ShipmentStatus,
  type ReturnStatus,
  type PaymentStatus,
} from "@/types/order";

/**
 * Centralised status presentation + transition rules.
 * Pure functions — keep UI components dumb and reusable.
 */

export interface StatusPresentation {
  label: string;
  /** Tailwind-compatible semantic token classes — use as-is, no overrides. */
  tone: "neutral" | "info" | "warning" | "success" | "danger" | "muted";
  description: string;
}

export const ORDER_STATUS_PRESENTATION: Record<OrderStatus, StatusPresentation> = {
  CREATED:           { label: "Created",           tone: "neutral", description: "Order created, awaiting confirmation" },
  CONFIRMED:         { label: "Confirmed",         tone: "info",    description: "Order confirmed by seller" },
  PROCESSING:        { label: "Processing",        tone: "info",    description: "Seller is preparing your order" },
  PARTIALLY_SHIPPED: { label: "Partially shipped", tone: "info",    description: "Some items have shipped" },
  SHIPPED:           { label: "Shipped",           tone: "info",    description: "All items dispatched" },
  DELIVERED:         { label: "Delivered",         tone: "success", description: "Order delivered" },
  CANCELLED:         { label: "Cancelled",         tone: "danger",  description: "Order cancelled" },
  RETURNED:          { label: "Returned",          tone: "muted",   description: "Items returned" },
  REFUNDED:          { label: "Refunded",          tone: "muted",   description: "Refund issued" },
};

export const SHIPMENT_STATUS_PRESENTATION: Record<ShipmentStatus, StatusPresentation> = {
  PACKING:          { label: "Packing",            tone: "neutral", description: "Items being packed" },
  READY_TO_SHIP:    { label: "Ready to ship",      tone: "info",    description: "Awaiting carrier pickup" },
  IN_TRANSIT:       { label: "In transit",         tone: "info",    description: "On the way" },
  OUT_FOR_DELIVERY: { label: "Out for delivery",   tone: "warning", description: "With delivery agent" },
  DELIVERED:        { label: "Delivered",          tone: "success", description: "Delivered to address" },
  FAILED_DELIVERY:  { label: "Delivery failed",    tone: "danger",  description: "Will reattempt" },
};

export const RETURN_STATUS_PRESENTATION: Record<ReturnStatus, StatusPresentation> = {
  REQUESTED: { label: "Requested", tone: "warning", description: "Awaiting seller approval" },
  APPROVED:  { label: "Approved",  tone: "info",    description: "Pickup scheduled" },
  REJECTED:  { label: "Rejected",  tone: "danger",  description: "Return not accepted" },
  PICKED_UP: { label: "Picked up", tone: "info",    description: "Item picked up" },
  REFUNDED:  { label: "Refunded",  tone: "success", description: "Refund issued" },
};

export const PAYMENT_STATUS_PRESENTATION: Record<PaymentStatus, StatusPresentation> = {
  PENDING:             { label: "Pending",             tone: "neutral", description: "Awaiting payment" },
  AUTHORIZED:          { label: "Authorized",          tone: "info",    description: "Authorized, not captured" },
  CAPTURED:            { label: "Paid",                tone: "success", description: "Payment captured" },
  FAILED:              { label: "Failed",              tone: "danger",  description: "Payment failed" },
  REFUND_PENDING:      { label: "Refund pending",      tone: "warning", description: "Refund initiated" },
  REFUNDED:            { label: "Refunded",            tone: "muted",   description: "Refund completed" },
  PARTIALLY_REFUNDED:  { label: "Partial refund",      tone: "muted",   description: "Partial refund issued" },
};

export const TONE_CLASSES: Record<StatusPresentation["tone"], string> = {
  neutral: "bg-muted text-muted-foreground",
  info:    "bg-primary/10 text-primary",
  warning: "bg-warning/10 text-warning",
  success: "bg-success/10 text-success",
  danger:  "bg-destructive/10 text-destructive",
  muted:   "bg-muted text-muted-foreground",
};

/* -------------------------------------------------------------------------- */
/* Transition rules                                                           */
/* -------------------------------------------------------------------------- */

const ORDER_TRANSITIONS: Record<OrderStatus, OrderStatus[]> = {
  CREATED:           ["CONFIRMED", "CANCELLED"],
  CONFIRMED:         ["PROCESSING", "CANCELLED"],
  PROCESSING:        ["PARTIALLY_SHIPPED", "SHIPPED", "CANCELLED"],
  PARTIALLY_SHIPPED: ["SHIPPED", "DELIVERED"],
  SHIPPED:           ["DELIVERED"],
  DELIVERED:         ["RETURNED"],
  CANCELLED:         ["REFUNDED"],
  RETURNED:          ["REFUNDED"],
  REFUNDED:          [],
};

export function canTransitionOrder(from: OrderStatus, to: OrderStatus): boolean {
  return ORDER_TRANSITIONS[from]?.includes(to) ?? false;
}

const SHIPMENT_TRANSITIONS: Record<ShipmentStatus, ShipmentStatus[]> = {
  PACKING:          ["READY_TO_SHIP"],
  READY_TO_SHIP:    ["IN_TRANSIT"],
  IN_TRANSIT:       ["OUT_FOR_DELIVERY", "FAILED_DELIVERY"],
  OUT_FOR_DELIVERY: ["DELIVERED", "FAILED_DELIVERY"],
  DELIVERED:        [],
  FAILED_DELIVERY:  ["OUT_FOR_DELIVERY", "READY_TO_SHIP"],
};

export function canTransitionShipment(from: ShipmentStatus, to: ShipmentStatus): boolean {
  return SHIPMENT_TRANSITIONS[from]?.includes(to) ?? false;
}

export function nextShipmentStatuses(from: ShipmentStatus): ShipmentStatus[] {
  return SHIPMENT_TRANSITIONS[from] ?? [];
}

/* Re-export status constants for convenience */
export { ORDER_STATUS, SHIPMENT_STATUS, RETURN_STATUS, PAYMENT_STATUS };