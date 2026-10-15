package com.firstclub.membership.core.query;

import com.firstclub.membership.data.mappers.UserMembershipResponseMapper;
import com.firstclub.membership.models.response.user.MembershipCatalogResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Catalog manager for the customer application. It never exposes admin-only status or perk
 * configuration.
 */
@Service
@Transactional(readOnly = true)
public class UserMembershipCatalogManager {
  private final MembershipTierFetchService membershipTierFetchService;
  private final MembershipPlanFetchService membershipPlanFetchService;
  private final MembershipPerkFetchService membershipPerkFetchService;
  private final UserMembershipResponseMapper userMembershipResponseMapper;

  public UserMembershipCatalogManager(
      MembershipTierFetchService membershipTierFetchService,
      MembershipPlanFetchService membershipPlanFetchService,
      MembershipPerkFetchService membershipPerkFetchService,
      UserMembershipResponseMapper userMembershipResponseMapper) {
    this.membershipTierFetchService = membershipTierFetchService;
    this.membershipPlanFetchService = membershipPlanFetchService;
    this.membershipPerkFetchService = membershipPerkFetchService;
    this.userMembershipResponseMapper = userMembershipResponseMapper;
  }

  public MembershipCatalogResponse getAvailableMembershipOptions() {
    var availableTiers = membershipTierFetchService.getAvailableForEnrollment();
    Set<java.util.UUID> tierIds =
        availableTiers.stream()
            .map(com.firstclub.membership.data.entity.MembershipTierEntity::getId)
            .collect(java.util.stream.Collectors.toSet());
    Map<java.util.UUID, List<com.firstclub.membership.data.entity.MembershipPlanEntity>>
        plansByTier = membershipPlanFetchService.getAvailableByTierIds(tierIds);
    var perksByTier = membershipPerkFetchService.getActiveByTierIds(tierIds);
    var tiers =
        availableTiers.stream()
            .map(
                tier -> {
                  var plans =
                      plansByTier.getOrDefault(tier.getId(), List.of()).stream()
                          .map(userMembershipResponseMapper::toAvailablePlan)
                          .toList();
                  var benefits =
                      perksByTier.getOrDefault(tier.getId(), List.of()).stream()
                          .map(userMembershipResponseMapper::toBenefit)
                          .toList();
                  return userMembershipResponseMapper.toAvailableTier(tier, plans, benefits);
                })
            .filter(tier -> !tier.plans().isEmpty())
            .toList();
    return new MembershipCatalogResponse(tiers);
  }
}
