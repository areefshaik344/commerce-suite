package com.commercesuite.shipping;
import com.commercesuite.shipping.entity.ShipmentStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShipmentStateMachineTest {
  @Test void transitions() {
    assertTrue(ShipmentStatus.CREATED.canTransitionTo(ShipmentStatus.READY_FOR_PICKUP));
    assertTrue(ShipmentStatus.IN_TRANSIT.canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY));
    assertTrue(ShipmentStatus.OUT_FOR_DELIVERY.canTransitionTo(ShipmentStatus.DELIVERED));
    assertFalse(ShipmentStatus.DELIVERED.canTransitionTo(ShipmentStatus.IN_TRANSIT));
    assertTrue(ShipmentStatus.DELIVERED.isTerminal());
  }
}
