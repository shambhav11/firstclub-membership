package com.firstclub.membership.core.query;

import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.data.repository.MembershipPerkRepository;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.PerkStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Focused active-perk reads reusable by customer-facing membership use cases. */
@Service
@Transactional(readOnly = true)
public class MembershipPerkFetchService {
  private final MembershipPerkRepository membershipPerkRepository;

  public MembershipPerkFetchService(MembershipPerkRepository membershipPerkRepository) {
    this.membershipPerkRepository = membershipPerkRepository;
  }

  public List<MembershipPerkEntity> getActiveForTier(
      UUID membershipTierId, Set<PerkAppliesTo> appliesTo) {
    return membershipPerkRepository.findAllByMembershipTierId(membershipTierId).stream()
        .filter(perk -> perk.getStatus() == PerkStatus.ACTIVE)
        .filter(perk -> appliesTo.isEmpty() || appliesTo.contains(perk.getAppliesTo()))
        .toList();
  }

  public List<MembershipPerkEntity> getAllForTier(UUID membershipTierId) {
    return membershipPerkRepository.findAllByMembershipTierId(membershipTierId);
  }

  public Map<UUID, List<MembershipPerkEntity>> getActiveByTierIds(
      java.util.Set<UUID> membershipTierIds) {
    return membershipPerkRepository.findActiveByMembershipTierIds(membershipTierIds);
  }

  public Page<MembershipPerkEntity> getPageForTier(UUID membershipTierId, Pageable pageable) {
    return membershipPerkRepository.findByMembershipTierId(membershipTierId, pageable);
  }

  public Page<MembershipPerkEntity> getPage(
      PerkStatus status,
      PerkAppliesTo appliesTo,
      String search,
      UUID unassignedToTierId,
      Pageable pageable) {
    return membershipPerkRepository.findPage(
        status, appliesTo, search, unassignedToTierId, pageable);
  }

  public Optional<MembershipPerkEntity> findActiveByAppliesToAndDisplayTextKey(
      PerkAppliesTo appliesTo, String displayTextKey) {
    return membershipPerkRepository.findActiveByAppliesToAndDisplayTextKey(appliesTo, displayTextKey);
  }
}
