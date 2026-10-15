package com.firstclub.membership.core.query;

import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.repository.MembershipPlanRepository;
import com.firstclub.membership.models.PlanStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Focused plan reads reusable by customer and membership lifecycle queries. */
@Service
@Transactional(readOnly = true)
public class MembershipPlanFetchService {
  private final MembershipPlanRepository membershipPlanRepository;

  public MembershipPlanFetchService(MembershipPlanRepository membershipPlanRepository) {
    this.membershipPlanRepository = membershipPlanRepository;
  }

  public MembershipPlanEntity getRequired(UUID membershipPlanId) {
    return membershipPlanRepository
        .findById(membershipPlanId)
        .orElseThrow(() -> new ResourceNotFoundException("Membership plan", membershipPlanId));
  }

  public List<MembershipPlanEntity> getAvailableForTier(UUID membershipTierId) {
    return membershipPlanRepository.findAllByMembershipTierId(membershipTierId).stream()
        .filter(plan -> plan.getStatus() == PlanStatus.ACTIVE && !plan.isEnrollmentClosed())
        .toList();
  }

  public List<MembershipPlanEntity> getAllForTier(UUID membershipTierId) {
    return membershipPlanRepository.findAllByMembershipTierId(membershipTierId);
  }

  public List<MembershipPlanEntity> findAllByIds(Set<UUID> ids) {
    return membershipPlanRepository.findAllByIds(ids);
  }

  public Page<MembershipPlanEntity> getPageForTier(UUID membershipTierId, Pageable pageable) {
    return membershipPlanRepository.findByMembershipTierId(membershipTierId, pageable);
  }

  public Map<UUID, MembershipPlanEntity> getByIds(Set<UUID> ids) {
    return membershipPlanRepository.findAllByIds(ids).stream()
        .collect(java.util.stream.Collectors.toMap(MembershipPlanEntity::getId, plan -> plan));
  }

  public Map<UUID, List<MembershipPlanEntity>> getAvailableByTierIds(Set<UUID> tierIds) {
    return membershipPlanRepository.findAvailableByMembershipTierIds(tierIds).stream()
        .collect(java.util.stream.Collectors.groupingBy(MembershipPlanEntity::getMembershipTierId));
  }
}
