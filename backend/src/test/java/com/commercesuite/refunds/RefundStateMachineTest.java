package com.commercesuite.refunds;
import com.commercesuite.refunds.entity.RefundStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefundStateMachineTest {
  @Test void transitions() {
    assertTrue(RefundStatus.PENDING.canTransitionTo(RefundStatus.APPROVED));
    assertTrue(RefundStatus.APPROVED.canTransitionTo(RefundStatus.PROCESSING));
    assertTrue(RefundStatus.PROCESSING.canTransitionTo(RefundStatus.COMPLETED));
    assertFalse(RefundStatus.COMPLETED.canTransitionTo(RefundStatus.PROCESSING));
    assertTrue(RefundStatus.COMPLETED.isTerminal());
  }
}
