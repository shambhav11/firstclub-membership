package com.firstclub.membership.data.repository.impl;

import com.firstclub.membership.data.dao.MembershipTierCriteriaDao;
import com.firstclub.membership.data.entity.MembershipTierCriteriaEntity;
import com.firstclub.membership.data.repository.MembershipTierCriteriaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipTierCriteriaRepositoryImpl implements MembershipTierCriteriaRepository {
  private final MembershipTierCriteriaDao membershipTierCriteriaDao;

  public MembershipTierCriteriaRepositoryImpl(MembershipTierCriteriaDao membershipTierCriteriaDao) {
    this.membershipTierCriteriaDao = membershipTierCriteriaDao;
  }

  @Override
  public Optional<MembershipTierCriteriaEntity> findByMembershipTierId(UUID membershipTierId) {
    return membershipTierCriteriaDao.findByMembershipTierId(membershipTierId);
  }

  @Override
  public Optional<MembershipTierCriteriaEntity> findByPriority(int priority) {
    return membershipTierCriteriaDao.findByPriority(priority);
  }

  @Override
  public List<MembershipTierCriteriaEntity> findAllOrderedByPriority() {
    return membershipTierCriteriaDao.findAllOrderedByPriority();
  }

  @Override
  public MembershipTierCriteriaEntity save(MembershipTierCriteriaEntity criteria) {
    return membershipTierCriteriaDao.save(criteria);
  }

  @Override
  public void flush() {
    membershipTierCriteriaDao.flush();
  }
}
