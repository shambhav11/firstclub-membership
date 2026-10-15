package com.firstclub.membership.models.response.admin;

import com.firstclub.membership.models.TierStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MembershipTierResponse(
    UUID id,
    String displayName,
    TierStatus status,
    boolean enrollmentClosed,
    List<MembershipPlanResponse> plans,
    List<TierPerkSummaryResponse> perks,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
