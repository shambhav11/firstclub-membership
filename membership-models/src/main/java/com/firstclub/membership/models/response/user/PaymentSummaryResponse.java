package com.firstclub.membership.models.response.user;

import com.firstclub.membership.models.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Does not expose gateway or payment-method references. */
public record PaymentSummaryResponse(
    UUID id,
    BigDecimal amount,
    String currencyCode,
    PaymentStatus status,
    OffsetDateTime paymentExpiresAt) {}
