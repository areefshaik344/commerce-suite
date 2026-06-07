package com.commercesuite.orders;
import com.commercesuite.orders.entity.OrderStatus;
import com.commercesuite.orders.entity.VendorOrderStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderStateMachineTest {
  @Test void parentTransitions() {
    assertTrue(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.CONFIRMED));
    assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.CONFIRMED));
    assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PROCESSING));
    assertTrue(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PARTIALLY_DELIVERED));
    assertTrue(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.COMPLETED));
    assertTrue(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PARTIALLY_RETURNED));
    assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.DELIVERED));
    assertTrue(OrderStatus.CANCELLED.isTerminal());
    assertTrue(OrderStatus.COMPLETED.isTerminal());
  }
  @Test void vendorTransitions() {
    assertTrue(VendorOrderStatus.PENDING_PAYMENT.canTransitionTo(VendorOrderStatus.CONFIRMED));
    assertTrue(VendorOrderStatus.CREATED.canTransitionTo(VendorOrderStatus.CONFIRMED));
    assertTrue(VendorOrderStatus.PACKED.canTransitionTo(VendorOrderStatus.SHIPPED));
    assertTrue(VendorOrderStatus.DELIVERED.canTransitionTo(VendorOrderStatus.RETURN_REQUESTED));
    assertTrue(VendorOrderStatus.DELIVERED.canTransitionTo(VendorOrderStatus.COMPLETED));
    assertTrue(VendorOrderStatus.REFUNDED.canTransitionTo(VendorOrderStatus.COMPLETED));
    assertFalse(VendorOrderStatus.SHIPPED.canTransitionTo(VendorOrderStatus.CANCELLED));
    assertTrue(VendorOrderStatus.PENDING_PAYMENT.isCancellable());
    assertTrue(VendorOrderStatus.PROCESSING.isCancellable());
    assertFalse(VendorOrderStatus.SHIPPED.isCancellable());
  }
}
