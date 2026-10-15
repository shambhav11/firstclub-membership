package com.firstclub.membership.models.response.user;

import com.firstclub.membership.models.RefundStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundSummaryResponse(
    UUID id,
    BigDecimal amount,
    String currencyCode,
    RefundStatus status,
    OffsetDateTime createdAt) {}
