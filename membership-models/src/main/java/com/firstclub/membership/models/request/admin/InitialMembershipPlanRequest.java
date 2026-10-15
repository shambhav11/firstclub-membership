package com.firstclub.membership.models.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record InitialMembershipPlanRequest(
    @Positive int durationDays,
    @NotBlank String displayText,
    @NotNull @PositiveOrZero BigDecimal amount,
    @NotBlank String currencyCode,
    boolean enrollmentClosed) {}
