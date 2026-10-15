package com.firstclub.membership.data.repository.impl;

import com.firstclub.membership.data.dao.MembershipTierPerkDao;
import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.data.entity.MembershipTierPerkEntity;
import com.firstclub.membership.data.repository.MembershipTierPerkRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipTierPerkRepositoryImpl implements MembershipTierPerkRepository {
  private final MembershipTierPerkDao membershipTierPerkDao;

  public MembershipTierPerkRepositoryImpl(MembershipTierPerkDao membershipTierPerkDao) {
    this.membershipTierPerkDao = membershipTierPerkDao;
  }

  @Override
  public Map<UUID, List<MembershipTierEntity>> findTiersByPerkIds(Set<UUID> membershipPerkIds) {
    return membershipTierPerkDao.findTiersByPerkIds(membershipPerkIds);
  }

  @Override
  public MembershipTierPerkEntity save(MembershipTierPerkEntity mapping) {
    return membershipTierPerkDao.save(mapping);
  }

  @Override
  public boolean exists(UUID tierId, UUID perkId) {
    return membershipTierPerkDao.exists(tierId, perkId);
  }

  @Override
  public boolean delete(UUID tierId, UUID perkId) {
    return membershipTierPerkDao.delete(tierId, perkId);
  }

  @Override
  public long countByPerkId(UUID perkId) {
    return membershipTierPerkDao.countByPerkId(perkId);
  }
}
