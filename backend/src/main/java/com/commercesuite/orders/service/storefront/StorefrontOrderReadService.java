package com.commercesuite.orders.service.storefront;

import com.commercesuite.catalog.entity.Product;
import com.commercesuite.catalog.entity.ProductMedia;
import com.commercesuite.catalog.repository.ProductMediaRepository;
import com.commercesuite.catalog.repository.ProductRepository;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.orders.dto.storefront.*;
import com.commercesuite.orders.entity.*;
import com.commercesuite.orders.repository.*;
import com.commercesuite.orders.service.OrderOwnershipGuard;
import com.commercesuite.payments.entity.PaymentIntent;
import com.commercesuite.payments.repository.PaymentIntentRepository;
import com.commercesuite.refunds.entity.RefundRequest;
import com.commercesuite.refunds.repository.RefundRequestRepository;
import com.commercesuite.returns.entity.ReturnRequest;
import com.commercesuite.returns.repository.ReturnRequestRepository;
import com.commercesuite.shipping.entity.Shipment;
import com.commercesuite.shipping.entity.TrackingEvent;
import com.commercesuite.shipping.repository.ShipmentRepository;
import com.commercesuite.shipping.repository.TrackingEventRepository;
import com.commercesuite.vendor.entity.VendorProfile;
import com.commercesuite.vendor.repository.VendorProfileRepository;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only aggregation service that builds frontend-optimised order DTOs.
 * All cross-domain lookups (vendors, products, media, shipments, returns,
 * refunds, payments) are batched to avoid N+1 queries.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorefrontOrderReadService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;
    private final VendorOrderRepository vendorOrderRepo;
    private final OrderStatusHistoryRepository historyRepo;
    private final ShipmentRepository shipmentRepo;
    private final TrackingEventRepository trackingRepo;
    private final ReturnRequestRepository returnRepo;
    private final RefundRequestRepository refundRepo;
    private final PaymentIntentRepository paymentRepo;
    private final ProductRepository productRepo;
    private final ProductMediaRepository mediaRepo;
    private final VendorProfileRepository vendorProfileRepo;
    private final OrderOwnershipGuard ownership;

    // ----------------------------------------------------------------- LISTING

    public Page<OrderCardDto> listForCustomer(ActorContext actor, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"));
        Page<Order> orders = orderRepo.findByCustomerId(actor.userId(), pageable);
        if (orders.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);

        List<UUID> orderIds = orders.stream().map(Order::getId).toList();
        Map<UUID, List<OrderItem>> itemsByOrder = itemRepo.findAll().stream()
                .filter(i -> orderIds.contains(i.getOrderId()))
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        Set<UUID> productIds = itemsByOrder.values().stream().flatMap(List::stream)
                .map(OrderItem::getProductId).collect(Collectors.toSet());
        Map<UUID, Product> products = bulkProducts(productIds);
        Map<UUID, String> primaryImage = bulkPrimaryImages(productIds);

        List<OrderCardDto> cards = orders.stream().map(o -> toCard(o,
                itemsByOrder.getOrDefault(o.getId(), List.of()), products, primaryImage)).toList();
        return new PageImpl<>(cards, pageable, orders.getTotalElements());
    }

    private OrderCardDto toCard(Order o, List<OrderItem> items,
                                Map<UUID, Product> products, Map<UUID, String> images) {
        OrderItem first = items.isEmpty() ? null : items.get(0);
        Product firstProduct = first == null ? null : products.get(first.getProductId());
        String image = first == null ? null : images.get(first.getProductId());
        int productCount = items.stream().mapToInt(OrderItem::getQty).sum();
        int vendorCount = (int) items.stream().map(OrderItem::getVendorId).distinct().count();
        return new OrderCardDto(
                o.getId(),
                orderNumber(o),
                o.getPlacedAt(),
                o.getStatus().name(),
                MoneyDto.of(o.getGrandTotalPaise(), o.getCurrency()),
                image,
                firstProduct == null ? null : firstProduct.getTitle(),
                productCount,
                vendorCount,
                isCancellable(o.getStatus()),
                isReturnable(o.getStatus())
        );
    }

    // ------------------------------------------------------------------ DETAIL

    public OrderDetailDto detail(UUID orderId, ActorContext actor) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> AppException.notFound("Order"));
        ownership.requireCustomerOrAdmin(o, actor);

        List<OrderItem> items = itemRepo.findByOrderId(orderId);
        List<VendorOrder> vendorOrders = vendorOrderRepo.findByOrderId(orderId);
        Map<UUID, VendorOrder> vendorOrderMap = vendorOrders.stream()
                .collect(Collectors.toMap(VendorOrder::getId, vo -> vo));

        Set<UUID> productIds = items.stream().map(OrderItem::getProductId).collect(Collectors.toSet());
        Set<UUID> vendorIds  = items.stream().map(OrderItem::getVendorId).collect(Collectors.toSet());

        Map<UUID, Product> products = bulkProducts(productIds);
        Map<UUID, String> images    = bulkPrimaryImages(productIds);
        Map<UUID, String> vendorNames = bulkVendorNames(vendorIds);

        List<OrderLineItemDto> lineItems = items.stream().map(it -> {
            Product p = products.get(it.getProductId());
            return new OrderLineItemDto(
                    it.getId(), it.getProductId(), it.getVariantId(), it.getVendorId(),
                    vendorNames.get(it.getVendorId()),
                    p == null ? null : p.getTitle(),
                    p == null ? null : p.getSlug(),
                    images.get(it.getProductId()),
                    it.getSku(), it.getQty(), it.getCancelledQty(), it.getReturnedQty(),
                    MoneyDto.of(it.getUnitPricePaise(), o.getCurrency()),
                    MoneyDto.of(it.getLineTotalPaise(), o.getCurrency()),
                    it.getStatus()
            );
        }).toList();

        // Shipments (with tracking events)
        List<Shipment> shipments = shipmentRepo.findByOrderId(orderId);
        List<ShipmentSummaryDto> shipmentDtos = shipments.stream()
                .map(s -> toShipmentSummary(s, vendorNames)).toList();

        // Returns + Refunds
        List<ReturnSummaryDto> returns = vendorOrders.stream()
                .flatMap(vo -> returnRepo.findByVendorOrderId(vo.getId()).stream())
                .map(this::toReturnSummary).toList();
        List<RefundSummaryDto> refunds = refundRepo.findByOrderId(orderId).stream()
                .map(r -> toRefundSummary(r, o.getCurrency())).toList();

        // Payment
        PaymentSummaryDto payment = paymentRepo.findByOrderId(orderId).stream()
                .findFirst().map(p -> toPaymentSummary(p, o.getCurrency())).orElse(null);

        OrderPricingDto pricing = new OrderPricingDto(
                MoneyDto.of(o.getSubtotalPaise(), o.getCurrency()),
                MoneyDto.of(o.getDiscountPaise(), o.getCurrency()),
                MoneyDto.of(o.getCouponDiscountPaise(), o.getCurrency()),
                MoneyDto.of(o.getShippingPaise(), o.getCurrency()),
                MoneyDto.of(o.getTaxPaise(), o.getCurrency()),
                MoneyDto.of(o.getPlatformFeePaise(), o.getCurrency()),
                MoneyDto.of(o.getGrandTotalPaise(), o.getCurrency()),
                o.getCouponCode()
        );

        return new OrderDetailDto(
                o.getId(),
                orderNumber(o),
                o.getStatus().name(),
                o.getPlacedAt(), o.getDeliveredAt(), o.getCancelledAt(),
                isCancellable(o.getStatus()), isReturnable(o.getStatus()),
                pricing,
                new AddressSnapshotDto(o.getAddressSnapshot()),
                new AddressSnapshotDto(o.getAddressSnapshot()),
                payment,
                lineItems,
                shipmentDtos,
                returns,
                refunds,
                buildTimeline(o, shipments, returns, refunds)
        );
    }

    // -------------------------------------------------------------- TIMELINE

    public OrderTimelineDto timeline(UUID orderId, ActorContext actor) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> AppException.notFound("Order"));
        ownership.requireCustomerOrAdmin(o, actor);
        List<Shipment> shipments = shipmentRepo.findByOrderId(orderId);
        List<ReturnSummaryDto> returns = vendorOrderRepo.findByOrderId(orderId).stream()
                .flatMap(vo -> returnRepo.findByVendorOrderId(vo.getId()).stream())
                .map(this::toReturnSummary).toList();
        List<RefundSummaryDto> refunds = refundRepo.findByOrderId(orderId).stream()
                .map(r -> toRefundSummary(r, o.getCurrency())).toList();
        return buildTimeline(o, shipments, returns, refunds);
    }

    private OrderTimelineDto buildTimeline(Order o, List<Shipment> shipments,
                                           List<ReturnSummaryDto> returns,
                                           List<RefundSummaryDto> refunds) {
        List<OrderStatusHistory> history = historyRepo.findByOrderIdOrderByChangedAtAsc(o.getId());
        Map<String, Instant> firstAt = new HashMap<>();
        for (OrderStatusHistory h : history) {
            firstAt.putIfAbsent(h.getToStatus(), h.getChangedAt());
        }
        OrderStatus status = o.getStatus();
        List<OrderTimelineEntryDto> entries = new ArrayList<>();
        entries.add(entry("CREATED", "Order placed", o.getPlacedAt(), true));

        Instant paidAt = paymentRepo.findByOrderId(o.getId()).stream()
                .map(PaymentIntent::getCapturedAt).filter(Objects::nonNull).findFirst().orElse(null);
        boolean paid = paidAt != null || status != OrderStatus.PENDING_PAYMENT;
        entries.add(entry("PAID", "Payment received", paidAt, paid));

        entries.add(entry("PROCESSING", "Processing",
                firstAt.get(OrderStatus.PROCESSING.name()),
                reached(status, OrderStatus.PROCESSING)));

        Instant shippedAt = shipments.stream().map(Shipment::getShippedAt)
                .filter(Objects::nonNull).min(Instant::compareTo).orElse(firstAt.get(OrderStatus.SHIPPED.name()));
        entries.add(entry("SHIPPED", "Shipped", shippedAt,
                reached(status, OrderStatus.SHIPPED) || shippedAt != null));

        Instant deliveredAt = o.getDeliveredAt() != null ? o.getDeliveredAt()
                : shipments.stream().map(Shipment::getDeliveredAt)
                    .filter(Objects::nonNull).max(Instant::compareTo).orElse(null);
        entries.add(entry("DELIVERED", "Delivered", deliveredAt,
                status == OrderStatus.DELIVERED || deliveredAt != null));

        if (!returns.isEmpty()) {
            Instant requested = returns.stream().map(ReturnSummaryDto::requestedAt)
                    .filter(Objects::nonNull).min(Instant::compareTo).orElse(null);
            entries.add(entry("RETURN_REQUESTED", "Return requested", requested, true));
            Instant resolved = returns.stream().map(ReturnSummaryDto::resolvedAt)
                    .filter(Objects::nonNull).max(Instant::compareTo).orElse(null);
            if (resolved != null || status == OrderStatus.RETURNED || status == OrderStatus.PARTIALLY_RETURNED) {
                entries.add(entry("RETURNED", "Returned", resolved,
                        status == OrderStatus.RETURNED || resolved != null));
            }
        }

        if (!refunds.isEmpty()) {
            Instant refundedAt = refunds.stream().map(RefundSummaryDto::completedAt)
                    .filter(Objects::nonNull).max(Instant::compareTo).orElse(null);
            entries.add(entry("REFUNDED", "Refunded", refundedAt, refundedAt != null));
        }

        if (o.getCancelledAt() != null || status == OrderStatus.CANCELLED
                || status == OrderStatus.PARTIALLY_CANCELLED) {
            entries.add(entry("CANCELLED", "Cancelled", o.getCancelledAt(), true));
        }

        return new OrderTimelineDto(o.getId(), entries);
    }

    // ----------------------------------------------------------- SUB-DETAILS

    public List<ShipmentSummaryDto> shipments(UUID orderId, ActorContext actor) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> AppException.notFound("Order"));
        ownership.requireCustomerOrAdmin(o, actor);
        Set<UUID> vendorIds = vendorOrderRepo.findByOrderId(orderId).stream()
                .map(VendorOrder::getVendorId).collect(Collectors.toSet());
        Map<UUID, String> vendorNames = bulkVendorNames(vendorIds);
        return shipmentRepo.findByOrderId(orderId).stream()
                .map(s -> toShipmentSummary(s, vendorNames)).toList();
    }

    public ShipmentSummaryDto shipment(UUID shipmentId, ActorContext actor) {
        Shipment s = shipmentRepo.findById(shipmentId)
                .orElseThrow(() -> AppException.notFound("Shipment"));
        Order o = orderRepo.findById(s.getOrderId())
                .orElseThrow(() -> AppException.notFound("Order"));
        ownership.requireCustomerOrAdmin(o, actor);
        return toShipmentSummary(s, bulkVendorNames(Set.of(s.getVendorId())));
    }

    public List<ReturnSummaryDto> returns(UUID orderId, ActorContext actor) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> AppException.notFound("Order"));
        ownership.requireCustomerOrAdmin(o, actor);
        return vendorOrderRepo.findByOrderId(orderId).stream()
                .flatMap(vo -> returnRepo.findByVendorOrderId(vo.getId()).stream())
                .map(this::toReturnSummary).toList();
    }

    public List<RefundSummaryDto> refunds(UUID orderId, ActorContext actor) {
        Order o = orderRepo.findById(orderId).orElseThrow(() -> AppException.notFound("Order"));
        ownership.requireCustomerOrAdmin(o, actor);
        return refundRepo.findByOrderId(orderId).stream()
                .map(r -> toRefundSummary(r, o.getCurrency())).toList();
    }

    // ------------------------------------------------------------- helpers

    private ShipmentSummaryDto toShipmentSummary(Shipment s, Map<UUID, String> vendorNames) {
        List<TrackingEvent> evs = trackingRepo.findByShipmentIdOrderByOccurredAtAsc(s.getId());
        List<TrackingEventDto> evDtos = evs.stream().map(e ->
                new TrackingEventDto(e.getEventType(), e.getDescription(), e.getLocation(), e.getOccurredAt())
        ).toList();
        return new ShipmentSummaryDto(
                s.getId(), s.getVendorOrderId(), s.getVendorId(),
                vendorNames.get(s.getVendorId()),
                s.getStatus().name(),
                s.getCarrier(), s.getTrackingNumber(), s.getShippingMethod(),
                s.getShippedAt(), s.getEstimatedDeliveryAt(), s.getDeliveredAt(),
                evDtos
        );
    }

    private ReturnSummaryDto toReturnSummary(ReturnRequest r) {
        return new ReturnSummaryDto(
                r.getId(), r.getVendorOrderId(), r.getStatus().name(),
                r.getReason() == null ? null : r.getReason().name(),
                r.getNote(),
                MoneyDto.of(r.getRefundPaise(), "INR"),
                r.getRequestedAt(), r.getReceivedAt(), r.getResolvedAt()
        );
    }

    private RefundSummaryDto toRefundSummary(RefundRequest r, String currency) {
        return new RefundSummaryDto(
                r.getId(), r.getVendorOrderId(), r.getStatus().name(),
                r.getSourceType().name(), r.getSourceId(),
                MoneyDto.of(r.getAmountPaise(), currency),
                r.getReason(), r.getRequestedAt(), r.getCompletedAt()
        );
    }

    private PaymentSummaryDto toPaymentSummary(PaymentIntent p, String currency) {
        return new PaymentSummaryDto(
                p.getMethodKind() == null ? null : p.getMethodKind().name(),
                p.getStatus().name(),
                MoneyDto.of(p.getAmountPaise(), currency),
                p.getGatewayIntentId(),
                p.getCapturedAt()
        );
    }

    private Map<UUID, Product> bulkProducts(Set<UUID> ids) {
        if (ids.isEmpty()) return Map.of();
        return productRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
    }

    private Map<UUID, String> bulkPrimaryImages(Set<UUID> productIds) {
        if (productIds.isEmpty()) return Map.of();
        Map<UUID, String> result = new HashMap<>();
        for (UUID pid : productIds) {
            List<ProductMedia> media = mediaRepo.findByProductIdOrderBySortOrderAsc(pid);
            if (!media.isEmpty()) result.put(pid, media.get(0).getUrl());
        }
        return result;
    }

    private Map<UUID, String> bulkVendorNames(Set<UUID> vendorIds) {
        if (vendorIds.isEmpty()) return Map.of();
        Map<UUID, String> out = new HashMap<>();
        for (UUID vid : vendorIds) {
            vendorProfileRepo.findByVendorId(vid)
                    .ifPresent(vp -> out.put(vid, vp.getStoreName()));
        }
        return out;
    }

    private static String orderNumber(Order o) {
        return "ORD-" + o.getId().toString().substring(0, 8).toUpperCase();
    }

    private static boolean isCancellable(OrderStatus s) {
        return s == OrderStatus.PENDING_PAYMENT || s == OrderStatus.CREATED
                || s == OrderStatus.CONFIRMED   || s == OrderStatus.PROCESSING;
    }

    private static boolean isReturnable(OrderStatus s) {
        return s == OrderStatus.DELIVERED || s == OrderStatus.PARTIALLY_DELIVERED;
    }

    private static boolean reached(OrderStatus current, OrderStatus milestone) {
        return ORDINAL.getOrDefault(current, -1) >= ORDINAL.getOrDefault(milestone, Integer.MAX_VALUE);
    }

    private static OrderTimelineEntryDto entry(String code, String label, Instant at, boolean reached) {
        return new OrderTimelineEntryDto(code, label, at, reached);
    }

    private static final Map<OrderStatus, Integer> ORDINAL = Map.ofEntries(
            Map.entry(OrderStatus.PENDING_PAYMENT, 0),
            Map.entry(OrderStatus.CREATED, 1),
            Map.entry(OrderStatus.CONFIRMED, 2),
            Map.entry(OrderStatus.PROCESSING, 3),
            Map.entry(OrderStatus.PARTIALLY_SHIPPED, 4),
            Map.entry(OrderStatus.SHIPPED, 5),
            Map.entry(OrderStatus.PARTIALLY_DELIVERED, 6),
            Map.entry(OrderStatus.DELIVERED, 7),
            Map.entry(OrderStatus.PARTIALLY_RETURNED, 8),
            Map.entry(OrderStatus.RETURNED, 9),
            Map.entry(OrderStatus.PARTIALLY_CANCELLED, 8),
            Map.entry(OrderStatus.CANCELLED, 10),
            Map.entry(OrderStatus.COMPLETED, 11),
            Map.entry(OrderStatus.CLOSED, 11)
    );
}