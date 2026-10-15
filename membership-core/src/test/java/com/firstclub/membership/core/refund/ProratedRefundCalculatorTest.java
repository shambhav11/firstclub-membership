package com.firstclub.membership.core.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ProratedRefundCalculatorTest {
  private final ProratedRefundCalculator calculator = new ProratedRefundCalculator();

  @Test
  void calculatesProratedRefundFromCalendarDays() {
    RefundQuote quote =
        calculator.calculate(
            new BigDecimal("3650.00"),
            365,
            Instant.parse("2026-01-01T10:00:00Z"),
            Instant.parse("2026-04-11T10:00:00Z"));
    assertEquals(100, quote.usedDays());
    assertEquals(265, quote.remainingDays());
    assertEquals(new BigDecimal("2650.00"), quote.amount());
  }

  @Test
  void givesFullRefundOnSameCalendarDay() {
    RefundQuote quote =
        calculator.calculate(
            new BigDecimal("199.00"),
            30,
            Instant.parse("2026-01-01T10:00:00Z"),
            Instant.parse("2026-01-01T23:00:00Z"));
    assertEquals(new BigDecimal("199.00"), quote.amount());
  }
}
