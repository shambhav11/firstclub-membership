package com.firstclub.membership.models.response.user;

import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.common.DisplayTextSegment;
import com.firstclub.membership.models.perk.PerkApplication;
import java.util.List;
import java.util.UUID;

/** A currently active perk with the typed configuration needed by a calling product surface. */
public record UserMembershipPerkResponse(
    UUID id,
    List<DisplayTextSegment> displayText,
    PerkAppliesTo appliesTo,
    PerkApplication perkApplication) {}
