package com.firstclub.membership.models.response.user;

import java.util.List;

/**
 * Customer-safe membership catalogue; admin status and configuration details are intentionally
 * omitted.
 */
public record MembershipCatalogResponse(List<AvailableMembershipTierResponse> tiers) {}
