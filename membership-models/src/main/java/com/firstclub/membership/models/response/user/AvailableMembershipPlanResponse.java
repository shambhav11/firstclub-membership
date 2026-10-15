package com.firstclub.membership.models.response.user;

import java.math.BigDecimal;
import java.util.UUID;

public record AvailableMembershipPlanResponse(
    UUID id, int durationDays, String displayText, BigDecimal amount, String currencyCode) {}
