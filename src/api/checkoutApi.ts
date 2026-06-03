/**
 * Checkout API — session lifecycle + inventory reservation mock.
 *
 * The session is the unit of work between cart and order. Backend will
 * persist it server-side; today we just round-trip with mock latency.
 */
import { simulateDelay, mockSuccess, ApiError, type ApiResponse } from "./apiClient";
import type {
  CartItem, CheckoutSession, OrderDraft, PaymentSelection, ReservationDto, ReservationLine, VendorShippingSelection,
} from "@/types/checkout";
import type { Address } from "@/data/mock-users";

/** Reservation window — backend will own this; mirrored here for UX. */
export const RESERVATION_TTL_MS = 10 * 60 * 1000; // 10 minutes

function reservationFromCart(ownerId: string, items: CartItem[]): ReservationDto {
  const lines: ReservationLine[] = items.map(it => ({
    productId: it.productId,
    variantId: it.variant?.variantId ?? it.productId,
    quantity: it.quantity,
  }));
  const now = Date.now();
  return {
    id: `RES-${now}-${Math.floor(Math.random() * 1e4)}`,
    ownerId,
    lines,
    status: "ACTIVE",
    createdAt: now,
    expiresAt: now + RESERVATION_TTL_MS,
  };
}

export interface CreateSessionRequest {
  ownerId: string;
  items: CartItem[];
}

export interface PlaceOrderRequest {
  draft: OrderDraft;
}

export interface PlaceOrderResult {
  orderId: string;
  placedAt: string;
}

export const checkoutApi = {
  async createSession(req: CreateSessionRequest): Promise<ApiResponse<CheckoutSession>> {
    await simulateDelay(250);
    if (req.items.length === 0) throw new ApiError("Cart is empty", 400, "EMPTY_CART");
    const now = Date.now();
    const session: CheckoutSession = {
      id: `CS-${now}`,
      ownerId: req.ownerId,
      step: "address",
      addressId: null,
      shippingByVendor: {},
      payment: null,
      reservation: null,
      appliedCoupons: [],
      pricingSnapshot: null,
      createdAt: now,
      updatedAt: now,
    };
    return mockSuccess(session, "Checkout session created");
  },

  async reserveInventory(ownerId: string, items: CartItem[]): Promise<ApiResponse<ReservationDto>> {
    await simulateDelay(300);
    const oos = items.filter(i => !i.product.inStock || i.quantity > i.product.stockCount);
    if (oos.length) {
      throw new ApiError("Some items are no longer available", 409, "INVENTORY_CONFLICT");
    }
    return mockSuccess(reservationFromCart(ownerId, items), "Inventory reserved");
  },

  async releaseReservation(reservationId: string): Promise<ApiResponse<{ released: true }>> {
    await simulateDelay(150);
    return mockSuccess({ released: true as const }, `Reservation ${reservationId} released`);
  },

  async setAddress(sessionId: string, addressId: string): Promise<ApiResponse<{ sessionId: string; addressId: string }>> {
    await simulateDelay(150);
    return mockSuccess({ sessionId, addressId });
  },

  async setShipping(sessionId: string, shipping: Record<string, VendorShippingSelection>): Promise<ApiResponse<typeof shipping>> {
    await simulateDelay(150);
    return mockSuccess(shipping);
  },

  async setPayment(sessionId: string, payment: PaymentSelection): Promise<ApiResponse<PaymentSelection>> {
    await simulateDelay(150);
    return mockSuccess(payment);
  },

  async placeOrder(req: PlaceOrderRequest): Promise<ApiResponse<PlaceOrderResult>> {
    await simulateDelay(700);
    // Simulated failure ~5% to exercise failure path.
    if (Math.random() < 0.05) {
      throw new ApiError("Payment authorization failed", 402, "PAYMENT_FAILED");
    }
    const id = `ORD-${Date.now().toString(36).toUpperCase()}`;
    return mockSuccess({ orderId: id, placedAt: new Date().toISOString() }, "Order placed");
  },

  /** Convenience: shape an OrderDraft on the client (immutable snapshot). */
  buildOrderDraft(args: {
    ownerId: string;
    address: Address;
    items: CartItem[];
    shipping: Record<string, VendorShippingSelection>;
    payment: PaymentSelection;
    reservationId: string | null;
    pricing: OrderDraft["pricing"];
  }): OrderDraft {
    const byVendor = new Map<string, CartItem[]>();
    args.items.forEach(it => {
      const list = byVendor.get(it.vendorId) ?? [];
      list.push(it);
      byVendor.set(it.vendorId, list);
    });
    const shipments = Array.from(byVendor.entries()).map(([vendorId, items]) => {
      const breakdown = args.pricing.vendorBreakdowns.find(v => v.vendorId === vendorId);
      return {
        vendorId,
        vendorName: items[0].vendorName,
        items: items.map(i => ({
          productId: i.productId,
          variantId: i.variant?.variantId ?? null,
          quantity: i.quantity,
          unitPrice: i.unitPriceSnapshot,
        })),
        shipping: args.shipping[vendorId],
        subtotal: breakdown?.subtotal ?? 0,
        discount: breakdown?.discount ?? 0,
        tax: breakdown?.tax ?? 0,
        total: breakdown?.total ?? 0,
      };
    });
    return {
      ownerId: args.ownerId,
      address: args.address,
      shipments,
      pricing: args.pricing,
      payment: args.payment,
      reservationId: args.reservationId,
      capturedAt: Date.now(),
    };
  },
};