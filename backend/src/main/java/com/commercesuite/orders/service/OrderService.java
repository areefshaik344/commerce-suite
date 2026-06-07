package com.commercesuite.orders.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.inventory.entity.ReservationReleaseReason;
import com.commercesuite.inventory.service.InventoryReservationService;
import com.commercesuite.orders.dto.*;
import com.commercesuite.orders.entity.*;
import com.commercesuite.orders.event.OrderEvents.OrderCancelledEvent;
import com.commercesuite.orders.event.OrderEvents.OrderDeliveredEvent;
import com.commercesuite.orders.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final VendorOrderRepository vendorOrderRepo;
    private final OrderItemRepository itemRepo;
    private final OrderStateMachine fsm;
    private final VendorOrderStateMachine voFsm;
    private final OrderRollupService rollup;
    private final OrderOwnershipGuard ownership;
    private final InventoryReservationService reservations;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional(readOnly = true)
    public OrderDto get(UUID orderId, ActorContext actor) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> AppException.notFound("Order"));
        ownership.requireCustomerOrAdmin(o, actor);
        return toDto(o);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> listForCustomer(UUID customerId, Pageable pageable) {
        return orderRepo.findByCustomerId(customerId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto> listAll(Pageable pageable) {
        return orderRepo.findAll(pageable).map(this::toDto);
    }

    /**
     * Customer-initiated cancellation. Cancels every still-cancellable vendor order,
     * releases any held reservations, then rolls up the parent.
     */
    @Transactional
    public OrderDto cancel(UUID orderId, CancelOrderRequest req, ActorContext actor) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> AppException.notFound("Order"));
        ownership.requireCustomerOrAdmin(o, actor);
        if (o.getStatus().isTerminal())
            throw AppException.conflict(ErrorCode.CONFLICT, "Order already terminal: " + o.getStatus());

        List<VendorOrder> children = vendorOrderRepo.findByOrderId(orderId);
        for (VendorOrder vo : children) {
            if (!vo.getStatus().isCancellable()) continue;
            voFsm.transition(vo, VendorOrderStatus.CANCELLED, actor.userId(), "customer",
                    req == null ? null : req.reason());
            vendorOrderRepo.save(vo);
            // Release any still-RESERVED inventory for this vendor's items (defensive)
            for (OrderItem it : itemRepo.findByVendorOrderId(vo.getId())) {
                if (it.getReservationId() != null) {
                    try { reservations.releaseBySystem(it.getReservationId(),
                            ReservationReleaseReason.EXPLICIT_RELEASE); }
                    catch (Exception ignored) {}
                }
            }
        }
        rollup.rollup(o);
        orderRepo.save(o);
        events.publishEvent(new OrderCancelledEvent(o.getId(), actor.userId(),
                req == null ? null : req.reason(), Instant.now(clock)));
        return toDto(o);
    }

    /** System or admin: mark order delivered when all children are delivered. */
    @Transactional
    public void markDelivered(UUID orderId) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> AppException.notFound("Order"));
        rollup.rollup(o);
        orderRepo.save(o);
        if (o.getStatus() == OrderStatus.DELIVERED)
            events.publishEvent(new OrderDeliveredEvent(o.getId(), Instant.now(clock)));
    }

    public OrderDto toDto(Order o) {
        List<VendorOrderDto> vendors = new ArrayList<>();
        for (VendorOrder vo : vendorOrderRepo.findByOrderId(o.getId())) {
            var items = itemRepo.findByVendorOrderId(vo.getId()).stream().map(OrderItemDto::from).toList();
            vendors.add(VendorOrderDto.from(vo, items));
        }
        return OrderDto.from(o, vendors);
    }
}
