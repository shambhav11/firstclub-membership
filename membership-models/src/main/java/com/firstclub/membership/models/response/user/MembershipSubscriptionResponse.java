package com.firstclub.membership.models.response.user;

import com.firstclub.membership.models.SubscriptionOrigin;
import com.firstclub.membership.models.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** The customer-facing view of one membership period. */
public record MembershipSubscriptionResponse(
    UUID id,
    UUID membershipTierId,
    String membershipTierDisplayName,
    UUID membershipPlanId,
    String membershipPlanDisplayText,
    int durationDays,
    BigDecimal membershipValueAmount,
    BigDecimal creditAppliedAmount,
    BigDecimal amountPaid,
    String currencyCode,
    boolean autoRenew,
    SubscriptionOrigin origin,
    SubscriptionStatus status,
    OffsetDateTime activationTimestamp,
    OffsetDateTime expiryTimestamp,
    OffsetDateTime cancelledAt,
    PaymentSummaryResponse payment,
    RefundSummaryResponse refund,
    OffsetDateTime createdAt) {}
