package com.commercesuite.shipping.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.orders.entity.*;
import com.commercesuite.orders.repository.*;
import com.commercesuite.orders.service.*;
import com.commercesuite.shipping.dto.*;
import com.commercesuite.shipping.entity.*;
import com.commercesuite.shipping.event.ShippingEvents.*;
import com.commercesuite.shipping.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepo;
    private final ShipmentItemRepository shipmentItemRepo;
    private final OrderItemRepository orderItemRepo;
    private final VendorOrderRepository vendorOrderRepo;
    private final OrderRepository orderRepo;
    private final ShipmentStateMachine fsm;
    private final VendorOrderStateMachine voFsm;
    private final OrderRollupService rollup;
    private final VendorOrderOwnershipGuard ownership;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public ShipmentDto create(CreateShipmentRequest req, ActorContext actor) {
        VendorOrder vo = vendorOrderRepo.findById(req.vendorOrderId())
                .orElseThrow(() -> AppException.notFound("VendorOrder"));
        ownership.requireVendorOrAdmin(vo, actor);
        if (vo.getStatus() != VendorOrderStatus.PROCESSING && vo.getStatus() != VendorOrderStatus.PACKED
                && vo.getStatus() != VendorOrderStatus.CONFIRMED)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Vendor order not ready for shipment: " + vo.getStatus());

        Shipment s = shipmentRepo.save(Shipment.builder()
                .orderId(vo.getOrderId()).vendorOrderId(vo.getId()).vendorId(vo.getVendorId())
                .status(ShipmentStatus.CREATED)
                .carrier(req.carrier()).trackingNumber(req.trackingNumber())
                .shippingMethod(req.shippingMethod())
                .shippingPaise(req.shippingPaise() == null ? 0L : req.shippingPaise())
                .build());

        List<ShipmentItemDto> items = new ArrayList<>();
        for (var spec : req.items()) {
            OrderItem oi = orderItemRepo.findById(spec.orderItemId())
                    .orElseThrow(() -> AppException.notFound("OrderItem"));
            if (!oi.getVendorOrderId().equals(vo.getId()))
                throw AppException.conflict(ErrorCode.CONFLICT,
                        "Order item not in this vendor order: " + oi.getId());
            if (spec.qty() > oi.getQty() - oi.getCancelledQty())
                throw AppException.conflict(ErrorCode.CONFLICT,
                        "Shipment qty exceeds remaining: " + oi.getId());
            ShipmentItem si = shipmentItemRepo.save(ShipmentItem.builder()
                    .shipmentId(s.getId()).orderItemId(oi.getId()).qty(spec.qty()).build());
            oi.setShipmentId(s.getId());
            orderItemRepo.save(oi);
            items.add(ShipmentItemDto.from(si));
        }

        events.publishEvent(new ShipmentCreatedEvent(s.getId(), vo.getId(), vo.getOrderId(), vo.getVendorId(),
                Instant.now(clock)));
        return ShipmentDto.from(s, items);
    }

    @Transactional
    public ShipmentDto transition(UUID shipmentId, ShipmentStatus next, ActorContext actor) {
        Shipment s = shipmentRepo.findById(shipmentId).orElseThrow(() -> AppException.notFound("Shipment"));
        VendorOrder vo = vendorOrderRepo.findById(s.getVendorOrderId()).orElseThrow();
        ownership.requireVendorOrAdmin(vo, actor);
        fsm.transition(s, next);
        shipmentRepo.save(s);

        // Propagate to vendor order
        if (next == ShipmentStatus.IN_TRANSIT && vo.getStatus().canTransitionTo(VendorOrderStatus.SHIPPED)) {
            voFsm.transition(vo, VendorOrderStatus.SHIPPED, actor.userId(), "vendor", "shipment in transit");
            vendorOrderRepo.save(vo);
        } else if (next == ShipmentStatus.OUT_FOR_DELIVERY
                && vo.getStatus().canTransitionTo(VendorOrderStatus.OUT_FOR_DELIVERY)) {
            voFsm.transition(vo, VendorOrderStatus.OUT_FOR_DELIVERY, actor.userId(), "vendor", "out for delivery");
            vendorOrderRepo.save(vo);
        } else if (next == ShipmentStatus.DELIVERED
                && vo.getStatus().canTransitionTo(VendorOrderStatus.DELIVERED)) {
            voFsm.transition(vo, VendorOrderStatus.DELIVERED, actor.userId(), "vendor", "delivered");
            vendorOrderRepo.save(vo);
            Order o = orderRepo.findById(vo.getOrderId()).orElseThrow();
            rollup.rollup(o);
            orderRepo.save(o);
            events.publishEvent(new ShipmentDeliveredEvent(s.getId(), s.getOrderId(), Instant.now(clock)));
        }
        return toDto(s);
    }

    @Transactional(readOnly = true)
    public ShipmentDto get(UUID id) {
        Shipment s = shipmentRepo.findById(id).orElseThrow(() -> AppException.notFound("Shipment"));
        return toDto(s);
    }

    @Transactional(readOnly = true)
    public List<ShipmentDto> listForVendorOrder(UUID vendorOrderId) {
        return shipmentRepo.findByVendorOrderId(vendorOrderId).stream().map(this::toDto).toList();
    }

    private ShipmentDto toDto(Shipment s) {
        var items = shipmentItemRepo.findByShipmentId(s.getId()).stream().map(ShipmentItemDto::from).toList();
        return ShipmentDto.from(s, items);
    }
}
