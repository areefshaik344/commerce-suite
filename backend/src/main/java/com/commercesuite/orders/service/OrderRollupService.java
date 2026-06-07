package com.commercesuite.orders.service;

import com.commercesuite.orders.entity.*;
import com.commercesuite.orders.repository.VendorOrderRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parent-order rollup per ORDER_FSM.md §3.
 * Recomputes the parent status from the union of its child vendor-order statuses.
 */
@Service
@RequiredArgsConstructor
public class OrderRollupService {
    private final VendorOrderRepository vendorOrderRepo;
    private final OrderStateMachine fsm;

    /**
     * HIGH H-02 resolution. Implements ORDER_FSM.md §3 rollup table verbatim,
     * using non-cancelled children as the denominator for "all" predicates.
     */
    @Transactional
    public void rollup(Order order) {
        List<VendorOrder> children = vendorOrderRepo.findByOrderId(order.getId());
        if (children.isEmpty()) return;

        int total       = children.size();
        int cancelled   = (int) children.stream().filter(c -> c.getStatus() == VendorOrderStatus.CANCELLED).count();
        int delivered   = (int) children.stream().filter(c -> c.getStatus() == VendorOrderStatus.DELIVERED).count();
        int shipped     = (int) children.stream().filter(c -> c.getStatus() == VendorOrderStatus.SHIPPED
                                                            || c.getStatus() == VendorOrderStatus.OUT_FOR_DELIVERY).count();
        int returned    = (int) children.stream().filter(c -> c.getStatus() == VendorOrderStatus.RETURNED).count();
        int refunded    = (int) children.stream().filter(c -> c.getStatus() == VendorOrderStatus.REFUNDED
                                                            || c.getStatus() == VendorOrderStatus.COMPLETED
                                                            || c.getStatus() == VendorOrderStatus.CLOSED).count();
        int nonCancelled = total - cancelled;

        OrderStatus target;
        if (cancelled == total) {
            target = OrderStatus.CANCELLED;
        } else if (nonCancelled > 0 && (delivered + returned + refunded) == nonCancelled) {
            if (returned + refunded == nonCancelled)       target = OrderStatus.RETURNED;
            else if (returned + refunded > 0)              target = OrderStatus.PARTIALLY_RETURNED;
            else                                            target = OrderStatus.DELIVERED;
        } else if (delivered > 0) {
            target = OrderStatus.PARTIALLY_DELIVERED;
        } else if (shipped == nonCancelled && nonCancelled > 0) {
            target = OrderStatus.SHIPPED;
        } else if (shipped > 0) {
            target = OrderStatus.PARTIALLY_SHIPPED;
        } else if (cancelled > 0) {
            target = OrderStatus.PARTIALLY_CANCELLED;
        } else {
            target = order.getStatus();
        }

        if (target != order.getStatus() && order.getStatus().canTransitionTo(target))
            fsm.transition(order, target, null, "system", "rollup");
    }
}
