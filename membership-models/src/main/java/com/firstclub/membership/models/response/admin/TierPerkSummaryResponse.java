package com.firstclub.membership.models.response.admin;

import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.common.DisplayTextSegment;
import com.firstclub.membership.models.perk.PerkApplication;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** The compact perk representation displayed within one membership tier's summary. */
public record TierPerkSummaryResponse(
    UUID id,
    List<DisplayTextSegment> displayText,
    PerkAppliesTo appliesTo,
    PerkApplication perkApplication,
    OffsetDateTime createdAt) {}
