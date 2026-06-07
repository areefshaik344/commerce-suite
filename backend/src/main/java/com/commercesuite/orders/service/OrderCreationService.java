package com.commercesuite.orders.service;

import com.commercesuite.cart.entity.Cart;
import com.commercesuite.cart.entity.CartItem;
import com.commercesuite.cart.entity.CartStatus;
import com.commercesuite.cart.repository.CartItemRepository;
import com.commercesuite.cart.repository.CartRepository;
import com.commercesuite.checkout.entity.CheckoutReservationLink;
import com.commercesuite.checkout.entity.CheckoutSession;
import com.commercesuite.checkout.entity.CheckoutStatus;
import com.commercesuite.checkout.repository.CheckoutReservationLinkRepository;
import com.commercesuite.checkout.repository.CheckoutSessionRepository;
import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.service.InventoryReservationService;
import com.commercesuite.orders.entity.*;
import com.commercesuite.orders.event.OrderEvents.OrderCreatedEvent;
import com.commercesuite.orders.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates an Order from a READY_FOR_ORDER CheckoutSession.
 *
 * Steps:
 *   1. Load + validate checkout (ownership, status, expiry).
 *   2. Snapshot product/vendor/address/pricing.
 *   3. Split cart items by vendor into VendorOrders.
 *   4. Commit all linked inventory reservations (RESERVATION_FSM.md: RESERVED → COMMITTED).
 *   5. Mark checkout CONVERTED and cart CONVERTED.
 *   6. Publish OrderCreatedEvent.
 */
@Service
@RequiredArgsConstructor
public class OrderCreationService {

    private final CheckoutSessionRepository checkoutRepo;
    private final CheckoutReservationLinkRepository linkRepo;
    private final CartRepository cartRepo;
    private final CartItemRepository cartItemRepo;
    private final OrderRepository orderRepo;
    private final VendorOrderRepository vendorOrderRepo;
    private final OrderItemRepository orderItemRepo;
    private final InventoryReservationService inventoryReservations;
    private final OrderSnapshotService snapshots;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Order create(UUID checkoutId, ActorContext actor) {
        CheckoutSession s = checkoutRepo.findById(checkoutId)
                .orElseThrow(() -> AppException.notFound("Checkout"));
        if (!s.getUserId().equals(actor.userId()))
            throw AppException.forbidden("Not your checkout");
        if (s.getStatus() != CheckoutStatus.READY_FOR_ORDER)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Checkout not READY_FOR_ORDER: " + s.getStatus());
        if (s.getExpiresAt().isBefore(Instant.now(clock)))
            throw AppException.conflict(ErrorCode.CONFLICT, "Checkout expired");

        // Idempotent: if order already exists for this checkout, return it.
        var existing = orderRepo.findByCheckoutId(checkoutId);
        if (existing.isPresent()) return existing.get();

        Cart cart = cartRepo.findById(s.getCartId())
                .orElseThrow(() -> AppException.conflict(ErrorCode.CONFLICT, "Cart not found"));
        List<CartItem> items = cartItemRepo.findByCartId(cart.getId());
        if (items.isEmpty())
            throw AppException.conflict(ErrorCode.CONFLICT, "Cart is empty");

        Instant now = Instant.now(clock);
        Order order = orderRepo.save(Order.builder()
                .customerId(actor.userId()).checkoutId(checkoutId)
                .status(OrderStatus.CREATED).currency(s.getCurrency())
                .subtotalPaise(s.getSubtotalPaise())
                .discountPaise(s.getDiscountPaise())
                .couponDiscountPaise(s.getCouponDiscountPaise())
                .shippingPaise(s.getShippingAmountPaise() == null ? 0L : s.getShippingAmountPaise())
                .taxPaise(s.getTaxPaise())
                .platformFeePaise(s.getPlatformFeePaise())
                .grandTotalPaise(s.getGrandTotalPaise())
                .couponCode(s.getCouponCode())
                .addressSnapshot(s.getAddressSnapshot() == null
                        ? snapshots.addressSnapshot(s.getAddressId()) : s.getAddressSnapshot())
                .pricingSnapshot(buildPricingSnapshot(s))
                .placedAt(now)
                .build());

        // Group cart items by vendor
        Map<UUID, List<CartItem>> byVendor = items.stream()
                .collect(Collectors.groupingBy(CartItem::getVendorId, LinkedHashMap::new, Collectors.toList()));

        // Map reservationId by variantId (1:1 within a checkout cart)
        Map<UUID, UUID> reservationByVariant = new HashMap<>();
        for (CheckoutReservationLink link : linkRepo.findByCheckoutIdAndActiveTrue(checkoutId)) {
            reservationByVariant.put(link.getVariantId(), link.getReservationId());
        }

        List<UUID> vendorOrderIds = new ArrayList<>();
        for (var e : byVendor.entrySet()) {
            UUID vendorId = e.getKey();
            List<CartItem> vItems = e.getValue();
            long subtotal = vItems.stream().mapToLong(ci -> Math.multiplyExact(ci.getUnitPricePaise(), ci.getQty())).sum();

            VendorOrder vo = vendorOrderRepo.save(VendorOrder.builder()
                    .orderId(order.getId()).vendorId(vendorId)
                    .status(VendorOrderStatus.CREATED)
                    .subtotalPaise(subtotal).discountPaise(0L)
                    .shippingPaise(0L).taxPaise(0L).totalPaise(subtotal)
                    .vendorSnapshot(snapshots.vendorSnapshot(vendorId))
                    .build());
            vendorOrderIds.add(vo.getId());

            for (CartItem ci : vItems) {
                long lineSubtotal = Math.multiplyExact(ci.getUnitPricePaise(), ci.getQty());
                UUID reservationId = reservationByVariant.get(ci.getVariantId());
                orderItemRepo.save(OrderItem.builder()
                        .orderId(order.getId()).vendorOrderId(vo.getId()).vendorId(vendorId)
                        .productId(ci.getProductId()).variantId(ci.getVariantId())
                        .reservationId(reservationId)
                        .qty(ci.getQty()).unitPricePaise(ci.getUnitPricePaise())
                        .lineSubtotalPaise(lineSubtotal).lineDiscountPaise(0L)
                        .lineTaxPaise(0L).lineTotalPaise(lineSubtotal)
                        .productSnapshot(snapshots.productSnapshot(ci.getProductId(), ci.getVariantId()))
                        .status("ACTIVE").build());
            }
        }

        // RESERVATION_FSM.md: commit RESERVED -> COMMITTED for each linked reservation.
        for (var link : linkRepo.findByCheckoutIdAndActiveTrue(checkoutId)) {
            inventoryReservations.commitBySystem(link.getReservationId());
            link.setActive(false);
            linkRepo.save(link);
        }

        // Close checkout + cart
        s.setStatus(CheckoutStatus.CONVERTED);
        checkoutRepo.save(s);
        cart.setStatus(CartStatus.CONVERTED);
        cartRepo.save(cart);

        events.publishEvent(new OrderCreatedEvent(order.getId(), actor.userId(), checkoutId,
                vendorOrderIds, order.getGrandTotalPaise(), now));
        return order;
    }

    private String buildPricingSnapshot(CheckoutSession s) {
        return "{\"subtotal\":" + s.getSubtotalPaise() +
               ",\"discount\":" + s.getDiscountPaise() +
               ",\"couponDiscount\":" + s.getCouponDiscountPaise() +
               ",\"shipping\":" + (s.getShippingAmountPaise() == null ? 0 : s.getShippingAmountPaise()) +
               ",\"tax\":" + s.getTaxPaise() +
               ",\"platformFee\":" + s.getPlatformFeePaise() +
               ",\"grandTotal\":" + s.getGrandTotalPaise() +
               ",\"currency\":\"" + s.getCurrency() + "\"}";
    }
}
