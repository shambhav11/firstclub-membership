package com.firstclub.membership.data.mappers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.data.entity.RefundEntity;
import com.firstclub.membership.models.common.DisplayTextSegment;
import com.firstclub.membership.models.perk.PerkApplication;
import com.firstclub.membership.models.response.user.AvailableMembershipPlanResponse;
import com.firstclub.membership.models.response.user.AvailableMembershipTierResponse;
import com.firstclub.membership.models.response.user.MembershipBenefitResponse;
import com.firstclub.membership.models.response.user.MembershipSubscriptionResponse;
import com.firstclub.membership.models.response.user.PaymentSummaryResponse;
import com.firstclub.membership.models.response.user.RefundSummaryResponse;
import com.firstclub.membership.models.response.user.UserMembershipPerkResponse;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserMembershipResponseMapper {
  private final ObjectMapper objectMapper;

  public UserMembershipResponseMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public AvailableMembershipTierResponse toAvailableTier(
      MembershipTierEntity tier,
      List<AvailableMembershipPlanResponse> plans,
      List<MembershipBenefitResponse> benefits) {
    return new AvailableMembershipTierResponse(
        tier.getId(), tier.getDisplayName(), plans, benefits);
  }

  public AvailableMembershipPlanResponse toAvailablePlan(MembershipPlanEntity plan) {
    return new AvailableMembershipPlanResponse(
        plan.getId(),
        plan.getDurationDays(),
        plan.getDisplayText(),
        plan.getAmount(),
        plan.getCurrencyCode());
  }

  public MembershipBenefitResponse toBenefit(MembershipPerkEntity perk) {
    return new MembershipBenefitResponse(perk.getId(), displayText(perk), perk.getAppliesTo());
  }

  public UserMembershipPerkResponse toUserPerk(MembershipPerkEntity perk) {
    return new UserMembershipPerkResponse(
        perk.getId(), displayText(perk), perk.getAppliesTo(), application(perk));
  }

  public MembershipSubscriptionResponse toSubscription(
      MembershipSubscriptionEntity subscription,
      MembershipPlanEntity plan,
      MembershipTierEntity tier,
      PaymentEntity payment,
      RefundEntity refund) {
    PaymentSummaryResponse paymentResponse = paymentSummary(payment);
    RefundSummaryResponse refundResponse = refundSummary(refund);
    return new MembershipSubscriptionResponse(
        subscription.getId(),
        tier.getId(),
        tier.getDisplayName(),
        plan.getId(),
        plan.getDisplayText(),
        plan.getDurationDays(),
        subscription.getMembershipValueAmount(),
        subscription.getCreditAppliedAmount(),
        subscription.getAmountPaid(),
        subscription.getCurrencyCode(),
        subscription.isAutoRenew(),
        subscription.getOrigin(),
        subscription.getStatus(),
        toIst(subscription.getActivationTimestamp()),
        toIst(subscription.getExpiryTimestamp()),
        toIst(subscription.getCancelledAt()),
        paymentResponse,
        refundResponse,
        toIst(subscription.getCreatedAt()));
  }

  private PaymentSummaryResponse paymentSummary(PaymentEntity payment) {
    if (payment == null) {
      return null;
    }
    return new PaymentSummaryResponse(
        payment.getId(),
        payment.getAmount(),
        payment.getCurrencyCode(),
        payment.getStatus(),
        toIst(payment.getPaymentExpiresAt()));
  }

  private RefundSummaryResponse refundSummary(RefundEntity refund) {
    if (refund == null) {
      return null;
    }
    return new RefundSummaryResponse(
        refund.getId(),
        refund.getAmount(),
        refund.getCurrencyCode(),
        refund.getStatus(),
        toIst(refund.getCreatedAt()));
  }

  private java.time.OffsetDateTime toIst(java.time.Instant instant) {
    return instant == null ? null : instant.atZone(ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
  }

  private List<DisplayTextSegment> displayText(MembershipPerkEntity perk) {
    return objectMapper.convertValue(
        perk.getDisplayText(), new TypeReference<List<DisplayTextSegment>>() {});
  }

  private PerkApplication application(MembershipPerkEntity perk) {
    try {
      return objectMapper.treeToValue(perk.getPerkApplication(), PerkApplication.class);
    } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
      throw new IllegalStateException("Invalid stored perk application", exception);
    }
  }
}
