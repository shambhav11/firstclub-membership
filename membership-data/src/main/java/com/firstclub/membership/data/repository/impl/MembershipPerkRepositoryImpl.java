package com.firstclub.membership.data.repository.impl;

import com.firstclub.membership.data.dao.MembershipPerkDao;
import com.firstclub.membership.data.entity.MembershipPerkEntity;
import com.firstclub.membership.data.repository.MembershipPerkRepository;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.PerkStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipPerkRepositoryImpl implements MembershipPerkRepository {
  private final MembershipPerkDao membershipPerkDao;

  public MembershipPerkRepositoryImpl(MembershipPerkDao membershipPerkDao) {
    this.membershipPerkDao = membershipPerkDao;
  }

  @Override
  public Page<MembershipPerkEntity> findPage(
      PerkStatus status,
      PerkAppliesTo appliesTo,
      String search,
      UUID unassignedToTierId,
      Pageable pageable) {
    return membershipPerkDao.findPage(status, appliesTo, search, unassignedToTierId, pageable);
  }

  @Override
  public Page<MembershipPerkEntity> findByMembershipTierId(
      UUID membershipTierId, Pageable pageable) {
    return membershipPerkDao.findByMembershipTierId(membershipTierId, pageable);
  }

  @Override
  public List<MembershipPerkEntity> findAllByMembershipTierId(UUID membershipTierId) {
    return membershipPerkDao.findAllByMembershipTierId(membershipTierId);
  }

  @Override
  public java.util.Map<UUID, List<MembershipPerkEntity>> findActiveByMembershipTierIds(
      java.util.Set<UUID> membershipTierIds) {
    return membershipPerkDao.findActiveByMembershipTierIds(membershipTierIds);
  }

  @Override
  public Optional<MembershipPerkEntity> findActiveByAppliesToAndDisplayTextKey(
      PerkAppliesTo appliesTo, String key) {
    return membershipPerkDao.findActiveByAppliesToAndDisplayTextKey(appliesTo, key);
  }

  @Override
  public Optional<MembershipPerkEntity> findById(UUID id) {
    return membershipPerkDao.findById(id);
  }

  @Override
  public MembershipPerkEntity save(MembershipPerkEntity perk) {
    return membershipPerkDao.save(perk);
  }
}
