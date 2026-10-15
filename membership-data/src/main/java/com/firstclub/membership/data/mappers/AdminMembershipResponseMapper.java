package com.firstclub.membership.data.mappers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipTierCriteriaEntity;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.models.common.DisplayTextSegment;
import com.firstclub.membership.models.perk.PerkApplication;
import com.firstclub.membership.models.response.admin.MembershipPerkResponse;
import com.firstclub.membership.models.response.admin.MembershipPlanResponse;
import com.firstclub.membership.models.response.admin.MembershipTierCriteriaResponse;
import com.firstclub.membership.models.response.admin.MembershipTierResponse;
import com.firstclub.membership.models.response.admin.MembershipTierSummaryResponse;
import com.firstclub.membership.models.response.admin.TierPerkSummaryResponse;
import com.firstclub.membership.models.response.admin.TierReferenceResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdminMembershipResponseMapper {
  private final ObjectMapper objectMapper;

  public AdminMembershipResponseMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public MembershipTierSummaryResponse toTierSummary(MembershipTierEntity entity) {
    return new MembershipTierSummaryResponse(
        entity.getId(),
        entity.getDisplayName(),
        entity.getStatus(),
        entity.isEnrollmentClosed(),
        toIst(entity.getCreatedAt()),
        toIst(entity.getUpdatedAt()));
  }

  public MembershipTierResponse toTierDetail(
      MembershipTierEntity tier,
      List<MembershipPlanResponse> plans,
      List<TierPerkSummaryResponse> perks) {
    return new MembershipTierResponse(
        tier.getId(),
        tier.getDisplayName(),
        tier.getStatus(),
        tier.isEnrollmentClosed(),
        plans,
        perks,
        toIst(tier.getCreatedAt()),
        toIst(tier.getUpdatedAt()));
  }

  public MembershipPlanResponse toPlan(MembershipPlanEntity entity) {
    return new MembershipPlanResponse(
        entity.getId(),
        entity.getMembershipTierId(),
        entity.getDurationDays(),
        entity.getDisplayText(),
        entity.getAmount(),
        entity.getCurrencyCode(),
        entity.getStatus(),
        entity.isEnrollmentClosed(),
        toIst(entity.getCreatedAt()),
        toIst(entity.getUpdatedAt()));
  }

  public MembershipTierCriteriaResponse toTierCriteria(MembershipTierCriteriaEntity entity) {
    return new MembershipTierCriteriaResponse(
        entity.getId(),
        entity.getMembershipTierId(),
        entity.getPriority(),
        entity.getGrantedPlanId(),
        entity.getEvaluationWindow(),
        entity.getExpression(),
        toIst(entity.getCreatedAt()),
        toIst(entity.getUpdatedAt()));
  }

  public MembershipPerkResponse toPerk(
      MembershipPerkEntity entity, List<MembershipTierEntity> tiers) {
    return new MembershipPerkResponse(
        entity.getId(),
        displayText(entity),
        entity.getAppliesTo(),
        application(entity),
        entity.getStatus(),
        tiers.stream().map(this::toTierReference).toList(),
        toIst(entity.getCreatedAt()),
        toIst(entity.getUpdatedAt()));
  }

  public TierPerkSummaryResponse toTierPerkSummary(MembershipPerkEntity entity) {
    return new TierPerkSummaryResponse(
        entity.getId(),
        displayText(entity),
        entity.getAppliesTo(),
        application(entity),
        toIst(entity.getCreatedAt()));
  }

  private TierReferenceResponse toTierReference(MembershipTierEntity entity) {
    return new TierReferenceResponse(entity.getId(), entity.getDisplayName());
  }

  private List<DisplayTextSegment> displayText(MembershipPerkEntity entity) {
    return objectMapper.convertValue(
        entity.getDisplayText(), new TypeReference<List<DisplayTextSegment>>() {});
  }

  private PerkApplication application(MembershipPerkEntity entity) {
    try {
      return objectMapper.treeToValue(entity.getPerkApplication(), PerkApplication.class);
    } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
      throw new IllegalStateException("Invalid stored perk application", exception);
    }
  }

  private OffsetDateTime toIst(Instant instant) {
    return instant == null ? null : instant.atZone(ZoneId.of("Asia/Kolkata")).toOffsetDateTime();
  }
}
