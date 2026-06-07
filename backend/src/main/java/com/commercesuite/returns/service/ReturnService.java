package com.commercesuite.returns.service;

import com.commercesuite.common.api.ErrorCode;
import com.commercesuite.common.audit.ActorContext;
import com.commercesuite.common.exception.AppException;
import com.commercesuite.orders.entity.*;
import com.commercesuite.orders.repository.*;
import com.commercesuite.orders.service.*;
import com.commercesuite.refunds.entity.RefundSourceType;
import com.commercesuite.refunds.service.RefundService;
import com.commercesuite.returns.dto.*;
import com.commercesuite.returns.entity.*;
import com.commercesuite.returns.event.ReturnEvents.*;
import com.commercesuite.returns.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final ReturnRequestRepository returnRepo;
    private final ReturnItemRepository returnItemRepo;
    private final OrderItemRepository orderItemRepo;
    private final VendorOrderRepository vendorOrderRepo;
    private final OrderRepository orderRepo;
    private final ReturnStateMachine fsm;
    private final VendorOrderStateMachine voFsm;
    private final OrderRollupService rollup;
    private final OrderOwnershipGuard customerOwnership;
    private final VendorOrderOwnershipGuard vendorOwnership;
    private final RefundService refundService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Value("${app.returns.window-days:7}")
    private long returnWindowDays;

    @Transactional
    public ReturnRequestDto create(CreateReturnRequest req, ActorContext actor) {
        VendorOrder vo = vendorOrderRepo.findById(req.vendorOrderId())
                .orElseThrow(() -> AppException.notFound("VendorOrder"));
        Order o = orderRepo.findById(vo.getOrderId()).orElseThrow();
        customerOwnership.requireCustomerOrAdmin(o, actor);

        if (vo.getStatus() != VendorOrderStatus.DELIVERED)
            throw AppException.conflict(ErrorCode.CONFLICT,
                    "Returns only allowed after delivery (status=" + vo.getStatus() + ")");
        // BUSINESS_RULES.md: return window check
        if (o.getDeliveredAt() != null
                && o.getDeliveredAt().plusSeconds(returnWindowDays * 86400L).isBefore(Instant.now(clock)))
            throw AppException.conflict(ErrorCode.CONFLICT, "Return window expired");

        long refundTotal = 0L;
        List<ReturnItem> persisted = new ArrayList<>();
        ReturnRequest rr = returnRepo.save(ReturnRequest.builder()
                .orderId(o.getId()).vendorOrderId(vo.getId()).vendorId(vo.getVendorId())
                .customerId(o.getCustomerId()).status(ReturnStatus.REQUESTED)
                .reason(req.reason()).note(req.note()).pickupAddressId(req.pickupAddressId())
                .refundPaise(0L).requestedAt(Instant.now(clock)).build());

        for (var spec : req.items()) {
            OrderItem oi = orderItemRepo.findById(spec.orderItemId())
                    .orElseThrow(() -> AppException.notFound("OrderItem"));
            if (!oi.getVendorOrderId().equals(vo.getId()))
                throw AppException.conflict(ErrorCode.CONFLICT, "Item not in this vendor order");
            int remaining = oi.getQty() - oi.getCancelledQty() - oi.getReturnedQty();
            if (spec.qty() > remaining)
                throw AppException.conflict(ErrorCode.CONFLICT, "Return qty exceeds remaining: " + oi.getId());
            long itemRefund = Math.multiplyExact(oi.getUnitPricePaise(), (long) spec.qty());
            refundTotal = Math.addExact(refundTotal, itemRefund);
            persisted.add(returnItemRepo.save(ReturnItem.builder()
                    .returnId(rr.getId()).orderItemId(oi.getId())
                    .qty(spec.qty()).refundPaise(itemRefund).build()));
        }
        rr.setRefundPaise(refundTotal);
        returnRepo.save(rr);

        // Move vendor order to RETURN_REQUESTED
        if (vo.getStatus().canTransitionTo(VendorOrderStatus.RETURN_REQUESTED)) {
            voFsm.transition(vo, VendorOrderStatus.RETURN_REQUESTED, actor.userId(), "customer", "return requested");
            vendorOrderRepo.save(vo);
        }

        events.publishEvent(new ReturnRequestedEvent(rr.getId(), o.getId(), vo.getId(), vo.getVendorId(),
                o.getCustomerId(), refundTotal, Instant.now(clock)));
        return toDto(rr, persisted);
    }

    @Transactional
    public ReturnRequestDto approve(UUID returnId, ActorContext actor) {
        ReturnRequest r = loadVendorAccessible(returnId, actor);
        fsm.transition(r, ReturnStatus.APPROVED, actor.userId());
        returnRepo.save(r);
        events.publishEvent(new ReturnApprovedEvent(r.getId(), actor.userId(), Instant.now(clock)));
        return toDto(r);
    }

    @Transactional
    public ReturnRequestDto reject(UUID returnId, ReturnDecisionRequest req, ActorContext actor) {
        ReturnRequest r = loadVendorAccessible(returnId, actor);
        fsm.transition(r, ReturnStatus.REJECTED, actor.userId());
        returnRepo.save(r);
        // Revert vendor order back to DELIVERED
        VendorOrder vo = vendorOrderRepo.findById(r.getVendorOrderId()).orElseThrow();
        if (vo.getStatus() == VendorOrderStatus.RETURN_REQUESTED
                && vo.getStatus().canTransitionTo(VendorOrderStatus.DELIVERED)) {
            voFsm.transition(vo, VendorOrderStatus.DELIVERED, actor.userId(), "vendor", "return rejected");
            vendorOrderRepo.save(vo);
        }
        events.publishEvent(new ReturnRejectedEvent(r.getId(), actor.userId(),
                req == null ? null : req.reason(), Instant.now(clock)));
        return toDto(r);
    }

    @Transactional
    public ReturnRequestDto markReceived(UUID returnId, ActorContext actor) {
        ReturnRequest r = loadVendorAccessible(returnId, actor);
        fsm.transition(r, ReturnStatus.RECEIVED, actor.userId());
        returnRepo.save(r);
        return toDto(r);
    }

    @Transactional
    public ReturnRequestDto complete(UUID returnId, ActorContext actor) {
        ReturnRequest r = loadVendorAccessible(returnId, actor);
        fsm.transition(r, ReturnStatus.COMPLETED, actor.userId());

        // Update order items: returnedQty + refundedPaise
        for (ReturnItem ri : returnItemRepo.findByReturnId(r.getId())) {
            OrderItem oi = orderItemRepo.findById(ri.getOrderItemId()).orElseThrow();
            oi.setReturnedQty(oi.getReturnedQty() + ri.getQty());
            oi.setRefundedPaise(oi.getRefundedPaise() + ri.getRefundPaise());
            orderItemRepo.save(oi);
        }

        // Transition vendor order to RETURNED
        VendorOrder vo = vendorOrderRepo.findById(r.getVendorOrderId()).orElseThrow();
        if (vo.getStatus().canTransitionTo(VendorOrderStatus.RETURNED)) {
            voFsm.transition(vo, VendorOrderStatus.RETURNED, actor.userId(), "vendor", "return completed");
            vendorOrderRepo.save(vo);
        }
        // Create refund request
        refundService.createForReturn(r);
        Order o = orderRepo.findById(r.getOrderId()).orElseThrow();
        rollup.rollup(o); orderRepo.save(o);
        returnRepo.save(r);
        events.publishEvent(new ReturnCompletedEvent(r.getId(), r.getRefundPaise(), Instant.now(clock)));
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public ReturnRequestDto get(UUID returnId, ActorContext actor) {
        ReturnRequest r = returnRepo.findById(returnId).orElseThrow(() -> AppException.notFound("Return"));
        if (!r.getCustomerId().equals(actor.userId())) {
            // vendor or admin
            VendorOrder vo = vendorOrderRepo.findById(r.getVendorOrderId()).orElseThrow();
            vendorOwnership.requireVendorOrAdmin(vo, actor);
        }
        return toDto(r);
    }

    @Transactional(readOnly = true)
    public Page<ReturnRequestDto> listForCustomer(UUID customerId, Pageable p) {
        return returnRepo.findByCustomerId(customerId, p).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReturnRequestDto> listForVendor(UUID vendorId, Pageable p) {
        return returnRepo.findByVendorId(vendorId, p).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReturnRequestDto> listAll(Pageable p) {
        return returnRepo.findAll(p).map(this::toDto);
    }

    private ReturnRequest loadVendorAccessible(UUID returnId, ActorContext actor) {
        ReturnRequest r = returnRepo.findById(returnId).orElseThrow(() -> AppException.notFound("Return"));
        VendorOrder vo = vendorOrderRepo.findById(r.getVendorOrderId()).orElseThrow();
        vendorOwnership.requireVendorOrAdmin(vo, actor);
        return r;
    }

    private ReturnRequestDto toDto(ReturnRequest r) {
        return toDto(r, returnItemRepo.findByReturnId(r.getId()));
    }

    private ReturnRequestDto toDto(ReturnRequest r, List<ReturnItem> items) {
        return ReturnRequestDto.from(r, items.stream().map(ReturnItemDto::from).toList());
    }
}
