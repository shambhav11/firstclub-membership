package com.firstclub.membership.core.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstclub.membership.core.query.AdminMembershipQueryManager;
import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.entity.MembershipTierPerkEntity;
import com.firstclub.membership.data.repository.MembershipPerkRepository;
import com.firstclub.membership.data.repository.MembershipPlanRepository;
import com.firstclub.membership.data.repository.MembershipTierPerkRepository;
import com.firstclub.membership.data.repository.MembershipTierRepository;
import com.firstclub.membership.models.PerkStatus;
import com.firstclub.membership.models.PlanStatus;
import com.firstclub.membership.models.TierStatus;
import com.firstclub.membership.models.request.admin.CreateMembershipTierRequest;
import com.firstclub.membership.models.response.admin.MembershipTierResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipWriteService {
  private final MembershipWriteValidator membershipWriteValidator;
  private final MembershipTierRepository tierRepository;
  private final MembershipPlanRepository planRepository;
  private final MembershipPerkRepository perkRepository;
  private final MembershipTierPerkRepository tierPerkRepository;
  private final AdminMembershipQueryManager adminMembershipQueryManager;
  private final ObjectMapper objectMapper;

  public MembershipWriteService(
      MembershipWriteValidator validator,
      MembershipTierRepository tierRepository,
      MembershipPlanRepository planRepository,
      MembershipPerkRepository perkRepository,
      MembershipTierPerkRepository tierPerkRepository,
      AdminMembershipQueryManager adminMembershipQueryManager,
      ObjectMapper objectMapper) {
    this.membershipWriteValidator = validator;
    this.tierRepository = tierRepository;
    this.planRepository = planRepository;
    this.perkRepository = perkRepository;
    this.tierPerkRepository = tierPerkRepository;
    this.adminMembershipQueryManager = adminMembershipQueryManager;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public MembershipTierResponse createTier(CreateMembershipTierRequest request) {
    membershipWriteValidator.validateCreateTier(request);
    MembershipTierEntity tier = createTierEntity(request);
    createPlans(tier, request);
    createPerkMappings(tier, request);
    tierRepository.flush();
    return adminMembershipQueryManager.getDetailedTierSummary(tier.getId());
  }

  private MembershipTierEntity createTierEntity(CreateMembershipTierRequest request) {
    MembershipTierEntity tier =
        new MembershipTierEntity(
            request.displayName().trim(), TierStatus.ACTIVE, request.enrollmentClosed());
    tier.recordCreation(request.requestedBy());
    return tierRepository.save(tier);
  }

  private void createPlans(MembershipTierEntity tier, CreateMembershipTierRequest request) {
    request.initialPlans().forEach(plan -> createPlan(tier, plan, request.requestedBy()));
  }

  private void createPlan(
      MembershipTierEntity tier,
      com.firstclub.membership.models.request.admin.InitialMembershipPlanRequest plan,
      String actor) {
    MembershipPlanEntity entity =
        new MembershipPlanEntity(
            tier.getId(),
            plan.durationDays(),
            plan.displayText().trim(),
            plan.amount(),
            plan.currencyCode().trim().toUpperCase(),
            PlanStatus.ACTIVE,
            plan.enrollmentClosed());
    entity.recordCreation(actor);
    planRepository.save(entity);
  }

  private void createPerkMappings(MembershipTierEntity tier, CreateMembershipTierRequest request) {
    request.perks().forEach(perk -> createPerkMapping(tier, perk, request.requestedBy()));
  }

  private void createPerkMapping(
      MembershipTierEntity tier,
      com.firstclub.membership.models.request.admin.NewMembershipPerkRequest perk,
      String actor) {
    MembershipPerkEntity resolved = findOrCreatePerk(perk, actor);
    MembershipTierPerkEntity mapping = new MembershipTierPerkEntity(tier.getId(), resolved.getId());
    mapping.recordCreation(actor);
    tierPerkRepository.save(mapping);
  }

  private MembershipPerkEntity findOrCreatePerk(
      com.firstclub.membership.models.request.admin.NewMembershipPerkRequest perk, String actor) {
    String key = membershipWriteValidator.displayTextKey(perk);
    return perkRepository
        .findActiveByAppliesToAndDisplayTextKey(perk.appliesTo(), key)
        .orElseGet(() -> createPerk(perk, key, actor));
  }

  private MembershipPerkEntity createPerk(
      com.firstclub.membership.models.request.admin.NewMembershipPerkRequest perk,
      String key,
      String actor) {
    MembershipPerkEntity entity =
        new MembershipPerkEntity(
            objectMapper.valueToTree(perk.displayText()),
            key,
            perk.appliesTo(),
            objectMapper.valueToTree(perk.perkApplication()),
            PerkStatus.ACTIVE);
    entity.recordCreation(actor);
    return perkRepository.save(entity);
  }
}
