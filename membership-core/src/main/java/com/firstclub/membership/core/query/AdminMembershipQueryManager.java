package com.firstclub.membership.core.query;

import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.mappers.AdminMembershipResponseMapper;
import com.firstclub.membership.data.mappers.PageResponseMapper;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.PerkStatus;
import com.firstclub.membership.models.TierStatus;
import com.firstclub.membership.models.common.PageResponse;
import com.firstclub.membership.models.response.admin.MembershipPerkResponse;
import com.firstclub.membership.models.response.admin.MembershipPlanResponse;
import com.firstclub.membership.models.response.admin.MembershipTierCriteriaResponse;
import com.firstclub.membership.models.response.admin.MembershipTierResponse;
import com.firstclub.membership.models.response.admin.MembershipTierSummaryResponse;
import com.firstclub.membership.models.response.admin.TierPerkSummaryResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminMembershipQueryManager {
  private final MembershipTierFetchService membershipTierFetchService;
  private final MembershipPlanFetchService membershipPlanFetchService;
  private final MembershipPerkFetchService membershipPerkFetchService;
  private final MembershipTierPerkFetchService membershipTierPerkFetchService;
  private final MembershipTierCriteriaFetchService membershipTierCriteriaFetchService;
  private final AdminMembershipResponseMapper responseMapper;
  private final PageResponseMapper pageResponseMapper;

  public AdminMembershipQueryManager(
      MembershipTierFetchService membershipTierFetchService,
      MembershipPlanFetchService membershipPlanFetchService,
      MembershipPerkFetchService membershipPerkFetchService,
      MembershipTierPerkFetchService membershipTierPerkFetchService,
      MembershipTierCriteriaFetchService membershipTierCriteriaFetchService,
      AdminMembershipResponseMapper responseMapper,
      PageResponseMapper pageResponseMapper) {
    this.membershipTierFetchService = membershipTierFetchService;
    this.membershipPlanFetchService = membershipPlanFetchService;
    this.membershipPerkFetchService = membershipPerkFetchService;
    this.membershipTierPerkFetchService = membershipTierPerkFetchService;
    this.membershipTierCriteriaFetchService = membershipTierCriteriaFetchService;
    this.responseMapper = responseMapper;
    this.pageResponseMapper = pageResponseMapper;
  }

  public PageResponse<MembershipTierSummaryResponse> getTiers(
      TierStatus status, Pageable pageable) {
    return pageResponseMapper.toResponse(
        membershipTierFetchService.getPage(status, pageable), responseMapper::toTierSummary);
  }

  public MembershipTierSummaryResponse getTier(UUID membershipTierId) {
    return responseMapper.toTierSummary(requireTier(membershipTierId));
  }

  public MembershipTierResponse getDetailedTierSummary(UUID membershipTierId) {
    MembershipTierEntity tier = requireTier(membershipTierId);
    List<MembershipPlanResponse> plans =
        membershipPlanFetchService.getAllForTier(membershipTierId).stream()
            .map(responseMapper::toPlan)
            .toList();
    List<TierPerkSummaryResponse> perks =
        membershipPerkFetchService.getAllForTier(membershipTierId).stream()
            .map(responseMapper::toTierPerkSummary)
            .toList();
    return responseMapper.toTierDetail(tier, plans, perks);
  }

  public PageResponse<MembershipPlanResponse> getTierPlans(
      UUID membershipTierId, Pageable pageable) {
    requireTier(membershipTierId);
    return pageResponseMapper.toResponse(
        membershipPlanFetchService.getPageForTier(membershipTierId, pageable),
        responseMapper::toPlan);
  }

  public MembershipTierCriteriaResponse getTierCriteria(UUID membershipTierId) {
    requireTier(membershipTierId);
    return membershipTierCriteriaFetchService
        .findByTierId(membershipTierId)
        .map(responseMapper::toTierCriteria)
        .orElseThrow(
            () -> new ResourceNotFoundException("Membership tier criteria", membershipTierId));
  }

  public PageResponse<MembershipPerkResponse> getTierPerks(
      UUID membershipTierId, Pageable pageable) {
    requireTier(membershipTierId);
    return toPerkPage(membershipPerkFetchService.getPageForTier(membershipTierId, pageable));
  }

  public PageResponse<MembershipPerkResponse> getPerks(
      PerkStatus status,
      PerkAppliesTo appliesTo,
      String search,
      UUID unassignedToTierId,
      Pageable pageable) {
    if (unassignedToTierId != null) requireTier(unassignedToTierId);
    return toPerkPage(
        membershipPerkFetchService.getPage(
            status, appliesTo, search, unassignedToTierId, pageable));
  }

  private PageResponse<MembershipPerkResponse> toPerkPage(Page<MembershipPerkEntity> page) {
    Set<UUID> perkIds =
        page.getContent().stream()
            .map(MembershipPerkEntity::getId)
            .collect(java.util.stream.Collectors.toSet());
    Map<UUID, List<MembershipTierEntity>> tiersByPerk =
        membershipTierPerkFetchService.findTiersByPerkIds(perkIds);
    return pageResponseMapper.toResponse(
        page,
        perk -> responseMapper.toPerk(perk, tiersByPerk.getOrDefault(perk.getId(), List.of())));
  }

  private MembershipTierEntity requireTier(UUID membershipTierId) {
    return membershipTierFetchService.getRequired(membershipTierId);
  }
}
