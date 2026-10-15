package com.firstclub.membership.models.response.user;

import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.common.DisplayTextSegment;
import java.util.List;
import java.util.UUID;

public record MembershipBenefitResponse(
    UUID id, List<DisplayTextSegment> displayText, PerkAppliesTo appliesTo) {}
