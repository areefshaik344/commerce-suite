package com.commercesuite.settlement;
import com.commercesuite.settlement.entity.SettlementStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettlementStateMachineTest {
  @Test void path() {
    assertTrue(SettlementStatus.PENDING.canTransitionTo(SettlementStatus.CALCULATED));
    assertTrue(SettlementStatus.CALCULATED.canTransitionTo(SettlementStatus.LOCKED));
    assertTrue(SettlementStatus.LOCKED.canTransitionTo(SettlementStatus.PAID));
    assertFalse(SettlementStatus.PAID.canTransitionTo(SettlementStatus.LOCKED));
    assertFalse(SettlementStatus.PENDING.canTransitionTo(SettlementStatus.PAID));
    assertTrue(SettlementStatus.PAID.isTerminal());
  }
}