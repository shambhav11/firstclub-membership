package com.firstclub.membership.models.response.user;

import java.util.List;
import java.util.UUID;

public record AvailableMembershipTierResponse(
    UUID id,
    String displayName,
    List<AvailableMembershipPlanResponse> plans,
    List<MembershipBenefitResponse> benefits) {}
