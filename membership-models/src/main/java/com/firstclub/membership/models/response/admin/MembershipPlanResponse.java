package com.firstclub.membership.models.response.admin;

import com.firstclub.membership.models.PlanStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipPlanResponse(
    UUID id,
    UUID membershipTierId,
    int durationDays,
    String displayText,
    BigDecimal amount,
    String currencyCode,
    PlanStatus status,
    boolean enrollmentClosed,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
