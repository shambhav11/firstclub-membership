package com.firstclub.membership.core.criteria;

import com.firstclub.membership.models.EvaluationWindow;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.stereotype.Component;

/** Produces explicit half-open timestamp ranges for criteria order aggregates. */
@Component
public class EvaluationWindowTimestampHelper {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
  private final Clock clock;

  public EvaluationWindowTimestampHelper(Clock clock) {
    this.clock = clock;
  }

  public TimestampRange forWindow(EvaluationWindow evaluationWindow) {
    ZonedDateTime startOfToday =
        ZonedDateTime.now(clock).withZoneSameInstant(BUSINESS_ZONE).toLocalDate().atStartOfDay(BUSINESS_ZONE);
    return switch (evaluationWindow) {
      case ROLLING_30_DAYS ->
          new TimestampRange(
              startOfToday.minusDays(30).toInstant(), startOfToday.toInstant());
      case MONTHLY -> {
        ZonedDateTime startOfThisMonth = startOfToday.withDayOfMonth(1);
        yield new TimestampRange(
            startOfThisMonth.minusMonths(1).toInstant(), startOfThisMonth.toInstant());
      }
      case YEAR_TO_DATE ->
          new TimestampRange(
              startOfToday.withDayOfYear(1).toInstant(), startOfToday.plusDays(1).toInstant());
      case LIFETIME -> new TimestampRange(Instant.EPOCH, startOfToday.plusDays(1).toInstant());
    };
  }

  public record TimestampRange(Instant fromInclusive, Instant toExclusive) {}
}
