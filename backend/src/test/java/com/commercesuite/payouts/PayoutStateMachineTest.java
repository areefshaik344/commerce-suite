package com.commercesuite.payouts;
import com.commercesuite.payouts.entity.PayoutStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PayoutStateMachineTest {
  @Test void path() {
    assertTrue(PayoutStatus.CREATED.canTransitionTo(PayoutStatus.PROCESSING));
    assertTrue(PayoutStatus.PROCESSING.canTransitionTo(PayoutStatus.COMPLETED));
    assertTrue(PayoutStatus.FAILED.canTransitionTo(PayoutStatus.PROCESSING));
    assertFalse(PayoutStatus.COMPLETED.canTransitionTo(PayoutStatus.FAILED));
    assertTrue(PayoutStatus.COMPLETED.isTerminal());
  }
}