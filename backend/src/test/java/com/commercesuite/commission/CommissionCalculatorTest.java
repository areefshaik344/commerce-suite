package com.commercesuite.commission;
import com.commercesuite.commission.entity.CommissionRule;
import com.commercesuite.commission.entity.CommissionType;
import com.commercesuite.commission.service.CommissionCalculator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommissionCalculatorTest {
  CommissionCalculator calc = new CommissionCalculator();

  @Test void percentageFloorsCorrectly() {
    CommissionRule r = CommissionRule.builder().ruleType(CommissionType.PERCENTAGE)
        .percentBps(500).minFeePaise(0L).active(true).build();
    // 12345 * 500 / 10000 = 617 (floor)
    assertEquals(617L, calc.compute(r, 12345L));
  }
  @Test void fixedAmount() {
    CommissionRule r = CommissionRule.builder().ruleType(CommissionType.FIXED_AMOUNT)
        .fixedPaise(1500L).minFeePaise(0L).active(true).build();
    assertEquals(1500L, calc.compute(r, 100000L));
  }
  @Test void minClamp() {
    CommissionRule r = CommissionRule.builder().ruleType(CommissionType.PERCENTAGE)
        .percentBps(100).minFeePaise(500L).active(true).build();
    assertEquals(500L, calc.compute(r, 1000L)); // raw 10, min 500
  }
  @Test void maxClamp() {
    CommissionRule r = CommissionRule.builder().ruleType(CommissionType.PERCENTAGE)
        .percentBps(5000).minFeePaise(0L).maxFeePaise(1000L).active(true).build();
    assertEquals(1000L, calc.compute(r, 100000L)); // raw 50000, capped 1000
  }
  @Test void zeroTaxable() { assertEquals(0L, calc.compute(null, 0L)); }
}