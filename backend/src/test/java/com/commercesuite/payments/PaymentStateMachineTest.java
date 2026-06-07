package com.commercesuite.payments;
import com.commercesuite.payments.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentStateMachineTest {
  @Test void happyPath() {
    assertTrue(PaymentStatus.CREATED.canTransitionTo(PaymentStatus.AUTHORIZED));
    assertTrue(PaymentStatus.AUTHORIZED.canTransitionTo(PaymentStatus.CAPTURED));
    assertTrue(PaymentStatus.CAPTURED.canTransitionTo(PaymentStatus.PARTIALLY_REFUNDED));
    assertTrue(PaymentStatus.PARTIALLY_REFUNDED.canTransitionTo(PaymentStatus.REFUNDED));
  }
  @Test void illegal() {
    assertFalse(PaymentStatus.REFUNDED.canTransitionTo(PaymentStatus.CAPTURED));
    assertFalse(PaymentStatus.FAILED.canTransitionTo(PaymentStatus.AUTHORIZED));
    assertFalse(PaymentStatus.CANCELLED.canTransitionTo(PaymentStatus.CAPTURED));
    assertTrue(PaymentStatus.REFUNDED.isTerminal());
  }
}