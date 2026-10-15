package com.firstclub.membership.models.request.admin;

import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateMembershipPlanRequest(
    @NotNull UUID membershipTierId,
    @Positive int durationDays,
    @NotBlank String displayText,
    @NotNull @PositiveOrZero BigDecimal amount,
    @NotBlank String currencyCode,
    boolean enrollmentClosed,
    @NotBlank String requestedBy)
    implements AuditedRequest {}
