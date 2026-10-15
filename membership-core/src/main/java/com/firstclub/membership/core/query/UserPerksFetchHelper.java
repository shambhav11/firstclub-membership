package com.firstclub.membership.core.query;

import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.mappers.UserMembershipResponseMapper;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.response.user.UserMembershipPerkResponse;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Internal assembler for operational-perk reads in the user-membership query use case. */
@Component
@Transactional(readOnly = true)
class UserPerksFetchHelper {
  private final MembershipPlanFetchService membershipPlanFetchService;
  private final MembershipPerkFetchService membershipPerkFetchService;
  private final UserMembershipResponseMapper userMembershipResponseMapper;

  UserPerksFetchHelper(
      MembershipPlanFetchService membershipPlanFetchService,
      MembershipPerkFetchService membershipPerkFetchService,
      UserMembershipResponseMapper userMembershipResponseMapper) {
    this.membershipPlanFetchService = membershipPlanFetchService;
    this.membershipPerkFetchService = membershipPerkFetchService;
    this.userMembershipResponseMapper = userMembershipResponseMapper;
  }

  List<UserMembershipPerkResponse> getActivePerks(
      MembershipSubscriptionEntity subscription, Set<PerkAppliesTo> appliesTo) {
    var plan = membershipPlanFetchService.getRequired(subscription.getMembershipPlanId());
    return membershipPerkFetchService
        .getActiveForTier(plan.getMembershipTierId(), appliesTo)
        .stream()
        .map(userMembershipResponseMapper::toUserPerk)
        .toList();
  }
}
