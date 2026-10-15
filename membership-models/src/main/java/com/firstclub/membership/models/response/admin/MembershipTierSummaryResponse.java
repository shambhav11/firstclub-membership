package com.firstclub.membership.models.response.admin;

import com.firstclub.membership.models.TierStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipTierSummaryResponse(
    UUID id,
    String displayName,
    TierStatus status,
    boolean enrollmentClosed,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
