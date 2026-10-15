package com.firstclub.membership.models.response.admin;

import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.PerkStatus;
import com.firstclub.membership.models.common.DisplayTextSegment;
import com.firstclub.membership.models.perk.PerkApplication;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MembershipPerkResponse(
    UUID id,
    List<DisplayTextSegment> displayText,
    PerkAppliesTo appliesTo,
    PerkApplication perkApplication,
    PerkStatus status,
    List<TierReferenceResponse> tiers,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
