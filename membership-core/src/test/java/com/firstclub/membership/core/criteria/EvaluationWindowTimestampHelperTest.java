package com.firstclub.membership.core.criteria;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.firstclub.membership.models.EvaluationWindow;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class EvaluationWindowTimestampHelperTest {
  private final EvaluationWindowTimestampHelper helper =
      new EvaluationWindowTimestampHelper(
          Clock.fixed(Instant.parse("2026-10-15T06:30:00Z"), ZoneOffset.UTC));

  @Test
  void returnsPreviousThirtyFullCalendarDaysForRollingWindow() {
    var range = helper.forWindow(EvaluationWindow.ROLLING_30_DAYS);

    assertEquals(Instant.parse("2026-09-14T18:30:00Z"), range.fromInclusive());
    assertEquals(Instant.parse("2026-10-14T18:30:00Z"), range.toExclusive());
  }

  @Test
  void returnsPreviousCompletedCalendarMonthForMonthlyWindow() {
    var range = helper.forWindow(EvaluationWindow.MONTHLY);

    assertEquals(Instant.parse("2026-08-31T18:30:00Z"), range.fromInclusive());
    assertEquals(Instant.parse("2026-09-30T18:30:00Z"), range.toExclusive());
  }

  @Test
  void returnsCurrentYearThroughTheEndOfTodayForYearToDateWindow() {
    var range = helper.forWindow(EvaluationWindow.YEAR_TO_DATE);

    assertEquals(Instant.parse("2025-12-31T18:30:00Z"), range.fromInclusive());
    assertEquals(Instant.parse("2026-10-15T18:30:00Z"), range.toExclusive());
  }
}
