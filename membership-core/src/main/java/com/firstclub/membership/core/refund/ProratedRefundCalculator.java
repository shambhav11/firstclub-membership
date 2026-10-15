package com.firstclub.membership.core.refund;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class ProratedRefundCalculator {
  public RefundQuote calculate(
      BigDecimal amountPaid, int durationDays, Instant activation, Instant cancellation) {
    int usedDays =
        (int)
            Math.min(
                durationDays,
                Math.max(
                    0,
                    ChronoUnit.DAYS.between(
                        activation.atZone(ZoneOffset.UTC).toLocalDate(),
                        cancellation.atZone(ZoneOffset.UTC).toLocalDate())));
    int remainingDays = durationDays - usedDays;
    BigDecimal amount =
        amountPaid
            .multiply(BigDecimal.valueOf(remainingDays))
            .divide(BigDecimal.valueOf(durationDays), 2, RoundingMode.HALF_UP);
    return new RefundQuote(usedDays, remainingDays, amount);
  }
}
