package com.firstclub.membership.models.response.user;

import java.math.BigDecimal;
import java.util.UUID;

/** Quote only; cancellation creates the actual pending refund, when applicable. */
public record MembershipCancellationQuoteResponse(
    UUID subscriptionId,
    int totalDurationDays,
    int usedDays,
    int remainingDays,
    BigDecimal refundAmount,
    String currencyCode) {}
