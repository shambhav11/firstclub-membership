package com.firstclub.membership.core.command;

import com.firstclub.membership.commons.ConflictException;
import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.commons.UserServiceInterface;
import com.firstclub.membership.core.query.MembershipPlanFetchService;
import com.firstclub.membership.core.query.MembershipSubscriptionFetchService;
import com.firstclub.membership.core.query.MembershipTierFetchService;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.models.PlanStatus;
import com.firstclub.membership.models.SubscriptionStatus;
import com.firstclub.membership.models.TierStatus;
import com.firstclub.membership.models.request.user.ChangeMembershipPlanRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MembershipPlanChangeValidator {
  private final UserServiceInterface userServiceInterface;
  private final MembershipSubscriptionFetchService subscriptionFetchService;
  private final MembershipPlanFetchService planFetchService;
  private final MembershipTierFetchService tierFetchService;

  public MembershipPlanChangeValidator(
      UserServiceInterface userServiceInterface,
      MembershipSubscriptionFetchService subscriptionFetchService,
      MembershipPlanFetchService planFetchService,
      MembershipTierFetchService tierFetchService) {
    this.userServiceInterface = userServiceInterface;
    this.subscriptionFetchService = subscriptionFetchService;
    this.planFetchService = planFetchService;
    this.tierFetchService = tierFetchService;
  }

  public ValidatedPlanChange validate(UUID userId, ChangeMembershipPlanRequest request) {
    requireUser(userId);
    MembershipSubscriptionEntity current = requireActiveSubscription(userId, request.currentSubscriptionId());
    rejectPendingPayment(userId);
    MembershipPlanEntity target = requirePlan(request.membershipPlanId());
    MembershipTierEntity tier = requireTier(target);
    validateTarget(current, target, tier);
    return new ValidatedPlanChange(current, target, tier);
  }

  private void requireUser(UUID userId) {
    if (!userServiceInterface.userExists(userId)) {
      throw new ResourceNotFoundException("User", userId);
    }
  }

  private MembershipSubscriptionEntity requireActiveSubscription(UUID userId, UUID subscriptionId) {
    MembershipSubscriptionEntity subscription =
        subscriptionFetchService
            .findById(subscriptionId)
            .orElseThrow(() -> new ResourceNotFoundException("Membership subscription", subscriptionId));
    if (!subscription.getUserId().equals(userId)) {
      throw new ResourceNotFoundException("Membership subscription", subscriptionId);
    }
    if (subscription.getStatus() != SubscriptionStatus.ACTIVE
        || !subscription.getExpiryTimestamp().isAfter(Instant.now())) {
      throw new ConflictException("The supplied current subscription is not active");
    }
    return subscription;
  }

  private void rejectPendingPayment(UUID userId) {
    if (subscriptionFetchService
        .findByUserIdAndStatus(userId, SubscriptionStatus.PENDING_PAYMENT)
        .isPresent()) {
      throw new ConflictException("A membership payment is already pending for this user");
    }
  }

  private MembershipPlanEntity requirePlan(UUID planId) {
    return planFetchService.getRequired(planId);
  }

  private MembershipTierEntity requireTier(MembershipPlanEntity plan) {
    return tierFetchService.getRequired(plan.getMembershipTierId());
  }

  private void validateTarget(
      MembershipSubscriptionEntity current, MembershipPlanEntity target, MembershipTierEntity tier) {
    if (target.getId().equals(current.getMembershipPlanId())) {
      throw new ConflictException("The target plan is already active");
    }
    if (!target.getCurrencyCode().equalsIgnoreCase(current.getCurrencyCode())) {
      throw new ConflictException("Plan changes across currencies are not supported");
    }
    if (target.getStatus() != PlanStatus.ACTIVE
        || target.isEnrollmentClosed()
        || tier.getStatus() != TierStatus.ACTIVE
        || tier.isEnrollmentClosed()) {
      throw new ConflictException("The selected membership plan is not available for enrollment");
    }
  }

  public record ValidatedPlanChange(
      MembershipSubscriptionEntity current,
      MembershipPlanEntity target,
      MembershipTierEntity tier) {}
}
