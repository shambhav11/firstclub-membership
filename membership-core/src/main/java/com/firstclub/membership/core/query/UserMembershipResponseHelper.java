package com.firstclub.membership.core.query;

import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.mappers.UserMembershipResponseMapper;
import com.firstclub.membership.models.response.user.MembershipSubscriptionResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Internal assembler for the user-membership query use case. */
@Component
@Transactional(readOnly = true)
class UserMembershipResponseHelper {
  private final MembershipPlanFetchService membershipPlanFetchService;
  private final MembershipTierFetchService membershipTierFetchService;
  private final PaymentFetchService paymentFetchService;
  private final RefundFetchService refundFetchService;
  private final UserMembershipResponseMapper userMembershipResponseMapper;

  UserMembershipResponseHelper(
      MembershipPlanFetchService membershipPlanFetchService,
      MembershipTierFetchService membershipTierFetchService,
      PaymentFetchService paymentFetchService,
      RefundFetchService refundFetchService,
      UserMembershipResponseMapper userMembershipResponseMapper) {
    this.membershipPlanFetchService = membershipPlanFetchService;
    this.membershipTierFetchService = membershipTierFetchService;
    this.paymentFetchService = paymentFetchService;
    this.refundFetchService = refundFetchService;
    this.userMembershipResponseMapper = userMembershipResponseMapper;
  }

  MembershipSubscriptionResponse toResponse(MembershipSubscriptionEntity subscription) {
    var plan = membershipPlanFetchService.getRequired(subscription.getMembershipPlanId());
    var tier = membershipTierFetchService.getRequired(plan.getMembershipTierId());
    return userMembershipResponseMapper.toSubscription(
        subscription,
        plan,
        tier,
        paymentFetchService.findLatestBySubscriptionId(subscription.getId()).orElse(null),
        refundFetchService.findBySubscriptionId(subscription.getId()).orElse(null));
  }

  List<MembershipSubscriptionResponse> toResponses(
      List<MembershipSubscriptionEntity> subscriptions) {
    if (subscriptions.isEmpty()) return List.of();
    Map<java.util.UUID, com.firstclub.membership.data.entity.MembershipPlanEntity> plans =
        membershipPlanFetchService.getByIds(
            subscriptions.stream()
                .map(MembershipSubscriptionEntity::getMembershipPlanId)
                .collect(java.util.stream.Collectors.toSet()));
    Map<java.util.UUID, com.firstclub.membership.data.entity.MembershipTierEntity> tiers =
        membershipTierFetchService.getByIds(
            plans.values().stream()
                .map(com.firstclub.membership.data.entity.MembershipPlanEntity::getMembershipTierId)
                .collect(java.util.stream.Collectors.toSet()));
    Set<java.util.UUID> subscriptionIds =
        subscriptions.stream()
            .map(MembershipSubscriptionEntity::getId)
            .collect(java.util.stream.Collectors.toSet());
    var payments = paymentFetchService.findLatestBySubscriptionIds(subscriptionIds);
    var refunds = refundFetchService.findBySubscriptionIds(subscriptionIds);
    return subscriptions.stream()
        .map(
            subscription ->
                userMembershipResponseMapper.toSubscription(
                    subscription,
                    plans.get(subscription.getMembershipPlanId()),
                    tiers.get(plans.get(subscription.getMembershipPlanId()).getMembershipTierId()),
                    payments.get(subscription.getId()),
                    refunds.get(subscription.getId())))
        .toList();
  }
}
