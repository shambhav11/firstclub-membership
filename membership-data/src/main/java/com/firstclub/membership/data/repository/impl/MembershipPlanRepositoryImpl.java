package com.firstclub.membership.data.repository.impl;

import com.firstclub.membership.data.dao.MembershipPlanDao;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.repository.MembershipPlanRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipPlanRepositoryImpl implements MembershipPlanRepository {
  private final MembershipPlanDao membershipPlanDao;

  public MembershipPlanRepositoryImpl(MembershipPlanDao membershipPlanDao) {
    this.membershipPlanDao = membershipPlanDao;
  }

  @Override
  public Page<MembershipPlanEntity> findByMembershipTierId(
      UUID membershipTierId, Pageable pageable) {
    return membershipPlanDao.findByMembershipTierId(membershipTierId, pageable);
  }

  @Override
  public List<MembershipPlanEntity> findAllByMembershipTierId(UUID membershipTierId) {
    return membershipPlanDao.findAllByMembershipTierId(membershipTierId);
  }

  @Override
  public List<MembershipPlanEntity> findAllByIds(java.util.Set<UUID> ids) {
    return membershipPlanDao.findAllByIds(ids);
  }

  @Override
  public List<MembershipPlanEntity> findAvailableByMembershipTierIds(java.util.Set<UUID> tierIds) {
    return membershipPlanDao.findAvailableByMembershipTierIds(tierIds);
  }

  @Override
  public MembershipPlanEntity save(MembershipPlanEntity plan) {
    return membershipPlanDao.save(plan);
  }

  @Override
  public Optional<MembershipPlanEntity> findById(UUID id) {
    return membershipPlanDao.findById(id);
  }
}
