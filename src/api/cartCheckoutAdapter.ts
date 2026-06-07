/**
 * Adapter: Spring Boot cart / checkout / coupon DTOs ↔ legacy frontend shapes.
 *
 * Centralises:
 *  - Money bridge (paise → rupees via `src/lib/money.ts`)
 *  - Backend `CheckoutSessionDto` → frontend `CheckoutSession` (best-effort,
 *    address / shipping / payment / coupons projected through existing UI types)
 *  - Backend `CouponValidationResult` → `ValidateCouponResult` (`AppliedCoupon`)
 *
 * Source contracts: backend/src/main/java/com/commercesuite/{cart,checkout,coupon}/dto/*
 */
import { paiseToRupees, rupeesToPaise } from "@/lib/money";
import type {
  AppliedCoupon,
  CheckoutSession,
  CouponDto,
  PricingBreakdown,
  VendorPricingBreakdown,
} from "@/types/checkout";

/* ----------------------------- backend DTOs ----------------------------- */

export type BackendCheckoutStatus =
  | "CREATED" | "ADDRESS_SELECTED" | "SHIPPING_SELECTED" | "PAYMENT_SELECTED"
  | "READY_FOR_ORDER" | "CONVERTED" | "EXPIRED" | "CANCELLED";

export type BackendShippingMethodKind = "STANDARD" | "EXPRESS" | "SAME_DAY" | "PICKUP" | "FREE_SHIPPING";
export type BackendPaymentMethodKind = "CARD" | "UPI" | "NETBANKING" | "WALLET" | "COD" | "EMI";

export interface BackendPricingBreakdown {
  subtotalPaise: number;
  discountPaise: number;
  couponDiscountPaise: number;
  shippingPaise: number;
  taxPaise: number;
  platformFeePaise: number;
  grandTotalPaise: number;
  currency: string;
}

export interface BackendCheckoutSessionDto {
  id: string;
  userId: string;
  cartId: string;
  status: BackendCheckoutStatus;
  currency: string;
  addressId: string | null;
  shippingMethod: BackendShippingMethodKind | null;
  shippingAmountPaise: number | null;
  paymentMethod: BackendPaymentMethodKind | null;
  couponCode: string | null;
  pricing: BackendPricingBreakdown;
  expiresAt: string;
  updatedAt: string;
}

export interface BackendCartItemDto {
  id: string; cartId: string; productId: string; variantId: string; vendorId: string;
  qty: number; unitPricePaise: number; lineTotalPaise: number; currency: string; addedAt: string;
}

export interface BackendCartDto {
  id: string; userId: string; status: "ACTIVE" | "MERGED" | "ABANDONED" | "CONVERTED";
  currency: string; subtotalPaise: number; totalItems: number;
  items: BackendCartItemDto[]; lastActivityAt: string; updatedAt: string;
}

export interface BackendCouponValidationResult {
  valid: boolean;
  code: string;
  discountPaise: number;
  subtotalPaise: number;
  grandTotalPaise: number;
  message: string;
}

/* ------------------------------ mappers ------------------------------ */

export function pricingFromBackend(p: BackendPricingBreakdown): PricingBreakdown {
  return {
    subtotal: paiseToRupees(p.subtotalPaise),
    discount: paiseToRupees(p.discountPaise + p.couponDiscountPaise),
    shipping: paiseToRupees(p.shippingPaise),
    tax: paiseToRupees(p.taxPaise),
    platformFee: paiseToRupees(p.platformFeePaise),
    grandTotal: paiseToRupees(p.grandTotalPaise),
    vendorBreakdowns: [] as VendorPricingBreakdown[],
    appliedCoupons: [],
    currency: "INR",
  };
}

export function sessionFromBackend(s: BackendCheckoutSessionDto): CheckoutSession {
  const status = s.status;
  let step: CheckoutSession["step"] = "address";
  if (status === "ADDRESS_SELECTED") step = "shipping";
  else if (status === "SHIPPING_SELECTED") step = "payment";
  else if (status === "PAYMENT_SELECTED" || status === "READY_FOR_ORDER") step = "review";
  else if (status === "CONVERTED") step = "review";

  const now = Date.now();
  return {
    id: s.id,
    ownerId: s.userId,
    step,
    addressId: s.addressId,
    shippingByVendor: {}, // backend stores one method globally — UI will fan-out
    payment: s.paymentMethod
      ? { method: s.paymentMethod.toLowerCase() as never, label: s.paymentMethod }
      : null,
    reservation: null, // reservations are server-owned and implicit on /start
    appliedCoupons: s.couponCode
      ? [{
          code: s.couponCode,
          label: s.couponCode,
          discount: paiseToRupees(s.pricing.couponDiscountPaise),
          type: "FLAT",
          scope: "GLOBAL",
          appliedAt: now,
        }]
      : [],
    pricingSnapshot: pricingFromBackend(s.pricing),
    createdAt: now,
    updatedAt: Date.parse(s.updatedAt) || now,
  };
}

export function couponResultToApplied(
  res: BackendCouponValidationResult,
  registryHint?: CouponDto | null,
): { coupon: CouponDto; applied: AppliedCoupon } {
  const coupon: CouponDto = registryHint ?? {
    code: res.code,
    label: res.code,
    type: "FLAT",
    value: paiseToRupees(res.discountPaise),
    scope: "GLOBAL",
    minOrder: 0,
    expiresAt: "",
    usageLimit: 0,
    usedCount: 0,
  };
  const applied: AppliedCoupon = {
    code: res.code,
    label: coupon.label,
    discount: paiseToRupees(res.discountPaise),
    type: coupon.type,
    scope: coupon.scope,
    vendorId: coupon.vendorId,
    appliedAt: Date.now(),
  };
  return { coupon, applied };
}

/** UUID-v4 sanity check — backend cart/checkout endpoints require UUIDs. */
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export const isUuid = (s: string | null | undefined): s is string => !!s && UUID_RE.test(s);

/** Idempotency-Key for `POST /checkout/start` and `POST /checkout/{id}/cancel`. */
export function idempotencyKey(prefix: string): string {
  // RFC4122-ish; sufficient for backend's IdempotencyKey.isValid check.
  const rand = (n: number) => Array.from({ length: n }, () => Math.floor(Math.random() * 16).toString(16)).join("");
  return `${prefix}-${rand(8)}-${rand(4)}-4${rand(3)}-${rand(4)}-${rand(12)}`;
}

export { paiseToRupees, rupeesToPaise };