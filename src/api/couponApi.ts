/**
 * Coupon API — backend-ready façade over the in-memory coupon registry.
 *
 * The frontend never owns coupon ledger state; this module simply
 * normalizes validation responses so stores can apply/remove optimistically.
 */
import { simulateDelay, mockSuccess, ApiError, type ApiResponse } from "./apiClient";
import type { AppliedCoupon, CouponDto } from "@/types/checkout";
import { computeCouponDiscount, isCouponStructurallyValid } from "@/lib/pricing";
import { httpClient, USE_REAL_API } from "./httpClient";
import { couponResultToApplied, type BackendCouponValidationResult } from "./cartCheckoutAdapter";

const REGISTRY: Record<string, CouponDto> = {
  SAVE10:        { code: "SAVE10",        label: "10% off (max ₹2000)",       type: "PERCENT", value: 10, scope: "GLOBAL", minOrder: 1000, maxDiscount: 2000, expiresAt: "2026-12-31", usageLimit: 1000, usedCount: 45 },
  FLAT500:       { code: "FLAT500",       label: "₹500 off orders over ₹3000", type: "FLAT",    value: 500, scope: "GLOBAL", minOrder: 3000, expiresAt: "2026-06-30", usageLimit: 500, usedCount: 20 },
  WELCOME:       { code: "WELCOME",       label: "15% off (new users, max ₹2000)", type: "PERCENT", value: 15, scope: "GLOBAL", minOrder: 500, maxDiscount: 2000, expiresAt: "2026-12-31", usageLimit: 99999, usedCount: 0 },
  FREEBIE:       { code: "FREEBIE",       label: "₹200 off",                  type: "FLAT",    value: 200, scope: "GLOBAL", minOrder: 0, expiresAt: "2026-03-31", usageLimit: 200, usedCount: 150 },
  ELECTRONICS20: { code: "ELECTRONICS20", label: "20% off electronics",        type: "PERCENT", value: 20, scope: "CATEGORY", categoryId: "electronics", minOrder: 5000, maxDiscount: 5000, expiresAt: "2026-06-30", usageLimit: 100, usedCount: 10 },
};

export interface ValidateCouponRequest {
  code: string;
  /** Amount the discount applies against (vendor-scoped subtotal or global subtotal). */
  baseAmount: number;
  vendorId?: string;
}

export interface ValidateCouponResult {
  coupon: CouponDto;
  applied: AppliedCoupon;
}

export const couponApi = {
  async listCoupons(): Promise<ApiResponse<CouponDto[]>> {
    await simulateDelay(200);
    return mockSuccess(Object.values(REGISTRY));
  },

  async getCoupon(code: string): Promise<ApiResponse<CouponDto | null>> {
    await simulateDelay(150);
    return mockSuccess(REGISTRY[code.trim().toUpperCase()] ?? null);
  },

  async validateCoupon(req: ValidateCouponRequest): Promise<ApiResponse<ValidateCouponResult>> {
    if (USE_REAL_API) {
      try {
        const res = await httpClient.post<BackendCouponValidationResult>(
          "/coupons/validate",
          { code: req.code.trim().toUpperCase() },
        );
        if (!res.data.valid) {
          throw new ApiError(res.data.message || "Coupon invalid", 400, "COUPON_INVALID");
        }
        const hint = REGISTRY[res.data.code.toUpperCase()] ?? null;
        return mockSuccess(couponResultToApplied(res.data, hint), res.data.message || "Coupon applied");
      } catch (err) {
        if (err instanceof ApiError) throw err; // surface backend rejection verbatim
      }
    }
    await simulateDelay(250);
    const coupon = REGISTRY[req.code.trim().toUpperCase()];
    if (!coupon) throw new ApiError("Invalid coupon code", 404, "COUPON_NOT_FOUND");
    if (!isCouponStructurallyValid(coupon)) throw new ApiError("Coupon has expired or reached its limit", 400, "COUPON_EXPIRED");
    if (coupon.scope === "VENDOR" && req.vendorId && coupon.vendorId !== req.vendorId) {
      throw new ApiError("Coupon does not apply to this vendor", 400, "COUPON_SCOPE_MISMATCH");
    }
    if (req.baseAmount < coupon.minOrder) {
      throw new ApiError(`Minimum order of ₹${coupon.minOrder} required`, 400, "COUPON_MIN_ORDER");
    }
    const discount = computeCouponDiscount(coupon, req.baseAmount);
    if (discount <= 0) throw new ApiError("Coupon does not provide a discount on this cart", 400, "COUPON_NO_DISCOUNT");
    const applied: AppliedCoupon = {
      code: coupon.code,
      label: coupon.label,
      discount,
      type: coupon.type,
      scope: coupon.scope,
      vendorId: coupon.vendorId,
      appliedAt: Date.now(),
    };
    return mockSuccess({ coupon, applied }, "Coupon applied");
  },
};