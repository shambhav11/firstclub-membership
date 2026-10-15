package com.firstclub.membership.data.repository.impl;

import com.firstclub.membership.data.dao.MembershipTierDao;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.repository.MembershipTierRepository;
import com.firstclub.membership.models.TierStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipTierRepositoryImpl implements MembershipTierRepository {
  private final MembershipTierDao membershipTierDao;

  public MembershipTierRepositoryImpl(MembershipTierDao membershipTierDao) {
    this.membershipTierDao = membershipTierDao;
  }

  @Override
  public Optional<MembershipTierEntity> findById(UUID id) {
    return membershipTierDao.findById(id);
  }

  @Override
  public List<MembershipTierEntity> findAllByIds(java.util.Set<UUID> ids) {
    return membershipTierDao.findAllByIds(ids);
  }

  @Override
  public Optional<MembershipTierEntity> findByDisplayNameIgnoreCase(String displayName) {
    return membershipTierDao.findByDisplayNameIgnoreCase(displayName);
  }

  @Override
  public MembershipTierEntity save(MembershipTierEntity tier) {
    return membershipTierDao.save(tier);
  }

  @Override
  public void flush() {
    membershipTierDao.flush();
  }

  @Override
  public Page<MembershipTierEntity> findPage(TierStatus status, Pageable pageable) {
    return membershipTierDao.findPage(status, pageable);
  }

  @Override
  public List<MembershipTierEntity> findAllAvailableForEnrollment() {
    return membershipTierDao.findAllAvailableForEnrollment();
  }
}
