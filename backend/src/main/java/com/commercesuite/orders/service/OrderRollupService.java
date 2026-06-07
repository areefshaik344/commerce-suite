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

    @Transactional
    public void rollup(Order order) {
        List<VendorOrder> children = vendorOrderRepo.findByOrderId(order.getId());
        if (children.isEmpty()) return;

        boolean allCancelled  = children.stream().allMatch(c -> c.getStatus() == VendorOrderStatus.CANCELLED);
        boolean anyCancelled  = children.stream().anyMatch(c -> c.getStatus() == VendorOrderStatus.CANCELLED);
        boolean allDelivered  = children.stream().allMatch(c -> isDeliveredOrTerminal(c.getStatus()));
        boolean anyDelivered  = children.stream().anyMatch(c -> c.getStatus() == VendorOrderStatus.DELIVERED
                                                              || c.getStatus() == VendorOrderStatus.RETURNED
                                                              || c.getStatus() == VendorOrderStatus.REFUNDED
                                                              || c.getStatus() == VendorOrderStatus.CLOSED);
        boolean anyShipped    = children.stream().anyMatch(c -> c.getStatus() == VendorOrderStatus.SHIPPED
                                                              || c.getStatus() == VendorOrderStatus.OUT_FOR_DELIVERY);
        boolean allShipped    = children.stream().allMatch(c -> isShippedOrLater(c.getStatus()) || c.getStatus() == VendorOrderStatus.CANCELLED);
        boolean allReturned   = children.stream().allMatch(c -> c.getStatus() == VendorOrderStatus.RETURNED
                                                              || c.getStatus() == VendorOrderStatus.REFUNDED
                                                              || c.getStatus() == VendorOrderStatus.CANCELLED);
        boolean anyReturned   = children.stream().anyMatch(c -> c.getStatus() == VendorOrderStatus.RETURNED
                                                              || c.getStatus() == VendorOrderStatus.REFUNDED);
        boolean allConfirmed  = children.stream().allMatch(c -> c.getStatus() != VendorOrderStatus.CREATED);

        OrderStatus target;
        if (allCancelled)                     target = OrderStatus.CANCELLED;
        else if (allReturned && anyReturned)  target = OrderStatus.RETURNED;
        else if (anyReturned)                 target = OrderStatus.PARTIALLY_RETURNED;
        else if (allDelivered && anyDelivered) target = OrderStatus.DELIVERED;
        else if (anyDelivered)                target = OrderStatus.PARTIALLY_SHIPPED;
        else if (allShipped && anyShipped)    target = OrderStatus.SHIPPED;
        else if (anyShipped)                  target = OrderStatus.PARTIALLY_SHIPPED;
        else if (anyCancelled)                target = OrderStatus.PARTIALLY_CANCELLED;
        else if (allConfirmed)                target = OrderStatus.CONFIRMED;
        else                                  target = order.getStatus();

        if (target != order.getStatus() && order.getStatus().canTransitionTo(target))
            fsm.transition(order, target, null, "system", "rollup");
    }

    private boolean isDeliveredOrTerminal(VendorOrderStatus s) {
        return s == VendorOrderStatus.DELIVERED || s == VendorOrderStatus.CANCELLED
            || s == VendorOrderStatus.RETURNED || s == VendorOrderStatus.REFUNDED || s == VendorOrderStatus.CLOSED;
    }
    private boolean isShippedOrLater(VendorOrderStatus s) {
        return s == VendorOrderStatus.SHIPPED || s == VendorOrderStatus.OUT_FOR_DELIVERY
            || s == VendorOrderStatus.DELIVERED || s == VendorOrderStatus.RETURNED
            || s == VendorOrderStatus.REFUNDED || s == VendorOrderStatus.CLOSED;
    }
}
