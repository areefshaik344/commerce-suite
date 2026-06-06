export type AnalyticsEventName =
  | "product_viewed"
  | "product_added_to_cart"
  | "product_removed_from_cart"
  | "checkout_started"
  | "checkout_step_completed"
  | "order_placed"
  | "payment_completed"
  | "search_performed"
  | "filter_applied";

export interface AnalyticsContext {
  userId?: string;
  anonymousId?: string;
  sessionId?: string;
  url?: string;
  referrer?: string;
}

export interface ProductViewedEvent {
  name: "product_viewed";
  productId: string;
  productName: string;
  category?: string;
  price: number;
  currency: string;
  vendorId?: string;
}

export interface ProductAddedToCartEvent {
  name: "product_added_to_cart";
  productId: string;
  variantId?: string;
  productName: string;
  price: number;
  quantity: number;
  currency: string;
}

export interface ProductRemovedFromCartEvent {
  name: "product_removed_from_cart";
  productId: string;
  variantId?: string;
  quantity: number;
}

export interface CheckoutStartedEvent {
  name: "checkout_started";
  checkoutId: string;
  itemCount: number;
  total: number;
  currency: string;
}

export interface CheckoutStepCompletedEvent {
  name: "checkout_step_completed";
  checkoutId: string;
  step: "address" | "shipping" | "payment" | "review";
}

export interface OrderPlacedEvent {
  name: "order_placed";
  orderId: string;
  total: number;
  itemCount: number;
  currency: string;
  vendorIds: string[];
}

export interface PaymentCompletedEvent {
  name: "payment_completed";
  paymentId: string;
  orderId: string;
  amount: number;
  currency: string;
  method: string;
}

export interface SearchPerformedEvent {
  name: "search_performed";
  query: string;
  resultsCount: number;
}

export interface FilterAppliedEvent {
  name: "filter_applied";
  facet: string;
  value: string | number;
}

export type AnalyticsEvent =
  | ProductViewedEvent
  | ProductAddedToCartEvent
  | ProductRemovedFromCartEvent
  | CheckoutStartedEvent
  | CheckoutStepCompletedEvent
  | OrderPlacedEvent
  | PaymentCompletedEvent
  | SearchPerformedEvent
  | FilterAppliedEvent;

export interface AnalyticsEnvelope<E extends AnalyticsEvent = AnalyticsEvent> {
  event: E;
  context: AnalyticsContext;
  at: string;
}