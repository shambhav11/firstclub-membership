package com.firstclub.membership.data.repository;

import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.PerkStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MembershipPerkRepository {
  Page<MembershipPerkEntity> findPage(
      PerkStatus status,
      PerkAppliesTo appliesTo,
      String search,
      UUID unassignedToTierId,
      Pageable pageable);

  Page<MembershipPerkEntity> findByMembershipTierId(UUID membershipTierId, Pageable pageable);

  List<MembershipPerkEntity> findAllByMembershipTierId(UUID membershipTierId);

  java.util.Map<UUID, List<MembershipPerkEntity>> findActiveByMembershipTierIds(
      java.util.Set<UUID> membershipTierIds);

  Optional<MembershipPerkEntity> findActiveByAppliesToAndDisplayTextKey(
      PerkAppliesTo appliesTo, String displayTextKey);

  Optional<MembershipPerkEntity> findById(UUID id);

  MembershipPerkEntity save(MembershipPerkEntity perk);
}
