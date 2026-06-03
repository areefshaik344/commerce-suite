/**
 * Centralized pricing engine.
 *
 * - Pure functions only; no store/UI coupling.
 * - Currency math kept in integers (₹) — round at the end of each line.
 * - Memoize at the selector level (see hooks/useCart, useCheckout).
 */
import { CURRENCY, formatPrice as formatPriceConst } from "@/config/constants";
import type {
  CartItem, AppliedCoupon, PricingBreakdown, VendorPricingBreakdown,
  VendorShippingSelection, CouponDto, ShippingOption,
} from "@/types/checkout";

export const PRICING_CONFIG = {
  taxRate: 0.18,                  // GST
  platformFeePercent: 0,          // disabled by default; backend will tune
  platformFeeMin: 0,
  defaultShippingCost: 49,
  freeShippingThreshold: 999,
  codSurcharge: 25,
} as const;

export const SHIPPING_OPTIONS: ShippingOption[] = [
  { id: "standard", label: "Standard", description: "Delivered in 4–6 business days", cost: 49,  estimatedDays: 5 },
  { id: "express",  label: "Express",  description: "Delivered in 1–2 business days", cost: 149, estimatedDays: 2 },
];

/* -------------------------------------------------------------------------- */
/* Money helpers                                                              */
/* -------------------------------------------------------------------------- */

export const formatPrice = formatPriceConst;

export function toMoney(v: number): number {
  return Math.max(0, Math.round(v));
}

/* -------------------------------------------------------------------------- */
/* Grouping                                                                   */
/* -------------------------------------------------------------------------- */

export interface VendorGroup {
  vendorId: string;
  vendorName: string;
  items: CartItem[];
}

export function groupItemsByVendor(items: CartItem[]): VendorGroup[] {
  const map = new Map<string, VendorGroup>();
  for (const it of items) {
    let g = map.get(it.vendorId);
    if (!g) {
      g = { vendorId: it.vendorId, vendorName: it.vendorName, items: [] };
      map.set(it.vendorId, g);
    }
    g.items.push(it);
  }
  return Array.from(map.values());
}

/* -------------------------------------------------------------------------- */
/* Coupon application                                                         */
/* -------------------------------------------------------------------------- */

/** Coupon expiry / usage / scope validity check. Does NOT validate minOrder. */
export function isCouponStructurallyValid(c: CouponDto, now: number = Date.now()): boolean {
  if (new Date(c.expiresAt).getTime() < now) return false;
  if (c.usedCount >= c.usageLimit) return false;
  return true;
}

export function computeCouponDiscount(c: CouponDto, baseAmount: number): number {
  if (baseAmount < c.minOrder) return 0;
  const raw = c.type === "PERCENT" ? (baseAmount * c.value) / 100 : c.value;
  const capped = c.maxDiscount ? Math.min(raw, c.maxDiscount) : raw;
  return toMoney(Math.min(capped, baseAmount));
}

/* -------------------------------------------------------------------------- */
/* Pricing breakdown                                                          */
/* -------------------------------------------------------------------------- */

export interface PricingInput {
  items: CartItem[];
  shipping: Record<string, VendorShippingSelection>;
  coupons: AppliedCoupon[];
  paymentSurcharge?: number;
}

function computeVendorSubtotal(items: CartItem[]): number {
  return items.reduce((s, i) => s + i.unitPriceSnapshot * i.quantity, 0);
}

function distributeGlobalDiscount(
  totalDiscount: number,
  vendorSubtotals: { vendorId: string; subtotal: number }[]
): Record<string, number> {
  const total = vendorSubtotals.reduce((s, v) => s + v.subtotal, 0);
  const out: Record<string, number> = {};
  if (total <= 0 || totalDiscount <= 0) {
    vendorSubtotals.forEach(v => (out[v.vendorId] = 0));
    return out;
  }
  let allocated = 0;
  vendorSubtotals.forEach((v, i) => {
    const share = i === vendorSubtotals.length - 1
      ? totalDiscount - allocated
      : toMoney((v.subtotal / total) * totalDiscount);
    out[v.vendorId] = share;
    allocated += share;
  });
  return out;
}

export function computePricing(input: PricingInput): PricingBreakdown {
  const groups = groupItemsByVendor(input.items);

  // Per-vendor subtotal
  const vendorSubtotals = groups.map(g => ({
    vendorId: g.vendorId,
    subtotal: computeVendorSubtotal(g.items),
  }));

  // Split coupons: vendor-scoped applied directly, global distributed proportionally.
  const vendorScoped: Record<string, number> = {};
  let globalDiscount = 0;
  for (const c of input.coupons) {
    if (c.scope === "VENDOR" && c.vendorId) {
      vendorScoped[c.vendorId] = (vendorScoped[c.vendorId] ?? 0) + c.discount;
    } else {
      globalDiscount += c.discount;
    }
  }
  const globalShare = distributeGlobalDiscount(globalDiscount, vendorSubtotals);

  const vendorBreakdowns: VendorPricingBreakdown[] = groups.map(g => {
    const subtotal = vendorSubtotals.find(v => v.vendorId === g.vendorId)!.subtotal;
    const discount = toMoney((vendorScoped[g.vendorId] ?? 0) + (globalShare[g.vendorId] ?? 0));
    const ship = input.shipping[g.vendorId];
    const shipping = ship ? ship.cost : (subtotal >= PRICING_CONFIG.freeShippingThreshold ? 0 : PRICING_CONFIG.defaultShippingCost);
    const taxBase = Math.max(0, subtotal - discount);
    const tax = toMoney(taxBase * PRICING_CONFIG.taxRate);
    const total = toMoney(subtotal - discount + shipping + tax);
    return {
      vendorId: g.vendorId,
      vendorName: g.vendorName,
      itemCount: g.items.reduce((s, i) => s + i.quantity, 0),
      subtotal,
      discount,
      shipping,
      tax,
      total,
    };
  });

  const subtotal = toMoney(vendorBreakdowns.reduce((s, v) => s + v.subtotal, 0));
  const discount = toMoney(vendorBreakdowns.reduce((s, v) => s + v.discount, 0));
  const shipping = toMoney(vendorBreakdowns.reduce((s, v) => s + v.shipping, 0));
  const tax = toMoney(vendorBreakdowns.reduce((s, v) => s + v.tax, 0));
  const platformFee = toMoney(
    Math.max(PRICING_CONFIG.platformFeeMin, subtotal * PRICING_CONFIG.platformFeePercent)
  );
  const surcharge = toMoney(input.paymentSurcharge ?? 0);
  const grandTotal = toMoney(subtotal - discount + shipping + tax + platformFee + surcharge);

  return {
    subtotal,
    discount,
    shipping,
    tax,
    platformFee: platformFee + surcharge,
    grandTotal,
    vendorBreakdowns,
    appliedCoupons: input.coupons,
    currency: CURRENCY.code as "INR",
    computedAt: Date.now(),
  };
}

export const EMPTY_PRICING: PricingBreakdown = {
  subtotal: 0, discount: 0, shipping: 0, tax: 0, platformFee: 0, grandTotal: 0,
  vendorBreakdowns: [], appliedCoupons: [], currency: "INR", computedAt: 0,
};