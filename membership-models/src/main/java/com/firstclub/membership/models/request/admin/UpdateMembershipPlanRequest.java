package com.firstclub.membership.models.request.admin;

import com.firstclub.membership.models.PlanStatus;
import com.firstclub.membership.models.request.AuditedRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateMembershipPlanRequest(
    @NotBlank String displayText,
    @NotNull @PositiveOrZero BigDecimal amount,
    @NotBlank String currencyCode,
    @NotNull PlanStatus status,
    boolean enrollmentClosed,
    @NotBlank String requestedBy)
    implements AuditedRequest {}
