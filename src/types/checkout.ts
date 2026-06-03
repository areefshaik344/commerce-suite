/**
 * Checkout + Cart domain — backend-ready DTOs.
 *
 * Frozen contracts. The mock API and the future backend MUST return these
 * shapes. Pricing snapshots are immutable so the order summary captured at
 * checkout time can be replayed exactly.
 *
 * Currency: all monetary values are integers in INR paise-free units (₹).
 * Switch to minor units (paise) only at the payment-gateway boundary.
 */
import type { Address } from "@/data/mock-users";
import type { Product } from "@/data/mock-products";

/* -------------------------------------------------------------------------- */
/* Cart                                                                       */
/* -------------------------------------------------------------------------- */

export interface CartItemVariantSnapshot {
  variantId: string;
  sku: string;
  /** option key -> option value, e.g. { Color: "Black", Size: "M" } */
  options: Record<string, string>;
}

export interface CartItem {
  /** Stable line id — `${productId}::${variantKey}` so identical variants dedupe. */
  lineId: string;
  productId: string;
  product: Product;
  variant: CartItemVariantSnapshot | null;
  quantity: number;
  /** Unit price captured when added — used for stale-pricing detection. */
  unitPriceSnapshot: number;
  vendorId: string;
  vendorName: string;
  /** Server-confirmed availability at last sync. */
  available: boolean;
  /** Mirrors product.stockCount or variant.inventory.stock at last sync. */
  stockAtSync: number;
  addedAt: number;
}

export interface SavedCartItem {
  lineId: string;
  productId: string;
  product: Product;
  variant: CartItemVariantSnapshot | null;
  savedAt: number;
}

export interface Cart {
  ownerId: string | null;
  items: CartItem[];
  saved: SavedCartItem[];
  updatedAt: number;
}

/* -------------------------------------------------------------------------- */
/* Coupons                                                                    */
/* -------------------------------------------------------------------------- */

export type CouponType = "PERCENT" | "FLAT";
export type CouponScope = "GLOBAL" | "VENDOR" | "CATEGORY";

export interface CouponDto {
  code: string;
  label: string;
  type: CouponType;
  value: number;
  scope: CouponScope;
  vendorId?: string;
  categoryId?: string;
  minOrder: number;
  maxDiscount?: number;
  expiresAt: string;
  usageLimit: number;
  usedCount: number;
}

export interface AppliedCoupon {
  code: string;
  label: string;
  discount: number;
  type: CouponType;
  scope: CouponScope;
  vendorId?: string;
  appliedAt: number;
}

/* -------------------------------------------------------------------------- */
/* Pricing                                                                    */
/* -------------------------------------------------------------------------- */

export interface VendorPricingBreakdown {
  vendorId: string;
  vendorName: string;
  itemCount: number;
  subtotal: number;
  discount: number;
  shipping: number;
  tax: number;
  total: number;
}

export interface PricingBreakdown {
  /** Sum of unit * qty across all items. */
  subtotal: number;
  /** All coupon + promo discounts combined. */
  discount: number;
  /** Sum of per-vendor shipping. */
  shipping: number;
  /** GST (18%) applied to (subtotal - discount). */
  tax: number;
  /** Marketplace platform fee. */
  platformFee: number;
  /** subtotal - discount + shipping + tax + platformFee */
  grandTotal: number;
  vendorBreakdowns: VendorPricingBreakdown[];
  appliedCoupons: AppliedCoupon[];
  currency: "INR";
  computedAt: number;
}

/* -------------------------------------------------------------------------- */
/* Shipping                                                                   */
/* -------------------------------------------------------------------------- */

export type ShippingMethodId = "standard" | "express";

export interface ShippingOption {
  id: ShippingMethodId;
  label: string;
  description: string;
  cost: number;
  estimatedDays: number;
}

export interface VendorShippingSelection {
  vendorId: string;
  methodId: ShippingMethodId;
  cost: number;
  estimatedDays: number;
}

/* -------------------------------------------------------------------------- */
/* Payment                                                                    */
/* -------------------------------------------------------------------------- */

export type PaymentMethodId = "cod" | "card" | "upi" | "wallet";

export interface PaymentMethodOption {
  id: PaymentMethodId;
  label: string;
  description: string;
  enabled: boolean;
  /** Charge added on top (e.g. COD handling fee). */
  surcharge?: number;
}

export interface PaymentSelection {
  methodId: PaymentMethodId;
  /** Opaque token returned by the gateway — placeholder for now. */
  token?: string;
}

/* -------------------------------------------------------------------------- */
/* Inventory Reservation                                                      */
/* -------------------------------------------------------------------------- */

export type ReservationStatus = "ACTIVE" | "EXPIRED" | "RELEASED" | "CONSUMED";

export interface ReservationLine {
  variantId: string;
  productId: string;
  quantity: number;
}

export interface ReservationDto {
  id: string;
  ownerId: string;
  lines: ReservationLine[];
  status: ReservationStatus;
  /** Epoch ms when this reservation auto-expires. */
  expiresAt: number;
  createdAt: number;
}

/* -------------------------------------------------------------------------- */
/* Checkout Session                                                           */
/* -------------------------------------------------------------------------- */

export type CheckoutStep = "address" | "shipping" | "payment" | "review";

export const CHECKOUT_STEPS: CheckoutStep[] = ["address", "shipping", "payment", "review"];

export interface CheckoutSession {
  id: string;
  ownerId: string;
  step: CheckoutStep;

  addressId: string | null;
  shippingByVendor: Record<string, VendorShippingSelection>;
  payment: PaymentSelection | null;

  reservation: ReservationDto | null;
  appliedCoupons: AppliedCoupon[];

  /** Pricing snapshot computed when entering review. */
  pricingSnapshot: PricingBreakdown | null;

  createdAt: number;
  updatedAt: number;
}

/* -------------------------------------------------------------------------- */
/* Order preparation DTO (forward-compat, NOT a full order record).           */
/* -------------------------------------------------------------------------- */

export interface OrderShipmentDraft {
  vendorId: string;
  vendorName: string;
  items: { productId: string; variantId: string | null; quantity: number; unitPrice: number }[];
  shipping: VendorShippingSelection;
  subtotal: number;
  discount: number;
  tax: number;
  total: number;
}

export interface OrderDraft {
  ownerId: string;
  address: Address;
  shipments: OrderShipmentDraft[];
  pricing: PricingBreakdown;
  payment: PaymentSelection;
  reservationId: string | null;
  /** Immutable snapshot the backend will persist on order create. */
  capturedAt: number;
}

/* -------------------------------------------------------------------------- */
/* Helpers                                                                    */
/* -------------------------------------------------------------------------- */

export function makeLineId(productId: string, variantId?: string | null): string {
  return `${productId}::${variantId ?? "default"}`;
}

export function isReservationExpired(r: ReservationDto | null | undefined, now: number = Date.now()): boolean {
  if (!r) return false;
  return r.status === "EXPIRED" || r.expiresAt <= now;
}

export function reservationSecondsLeft(r: ReservationDto | null | undefined, now: number = Date.now()): number {
  if (!r) return 0;
  return Math.max(0, Math.floor((r.expiresAt - now) / 1000));
}