package com.commercesuite.returns;
import com.commercesuite.returns.entity.ReturnStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReturnStateMachineTest {
  @Test void transitions() {
    assertTrue(ReturnStatus.REQUESTED.canTransitionTo(ReturnStatus.APPROVED));
    assertTrue(ReturnStatus.APPROVED.canTransitionTo(ReturnStatus.RECEIVED));
    assertTrue(ReturnStatus.RECEIVED.canTransitionTo(ReturnStatus.COMPLETED));
    assertFalse(ReturnStatus.REJECTED.canTransitionTo(ReturnStatus.COMPLETED));
    assertTrue(ReturnStatus.COMPLETED.isTerminal());
  }
}
