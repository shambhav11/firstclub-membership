package com.firstclub.membership.models.response.user;

import java.math.BigDecimal;
import java.util.UUID;

/** Quote only; it does not reserve a plan, charge a user, or change a membership. */
public record MembershipChangeQuoteResponse(
    UUID currentSubscriptionId,
    UUID currentMembershipPlanId,
    UUID targetMembershipPlanId,
    int currentPlanDurationDays,
    int usedDays,
    int remainingDays,
    BigDecimal unusedMembershipCredit,
    BigDecimal targetPlanAmount,
    BigDecimal creditAppliedAmount,
    BigDecimal amountDue,
    BigDecimal refundAmount,
    String currencyCode) {}
