package com.firstclub.membership.data.repository;

import com.firstclub.membership.data.entity.MembershipTierCriteriaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipTierCriteriaRepository {
  Optional<MembershipTierCriteriaEntity> findByMembershipTierId(UUID membershipTierId);

  Optional<MembershipTierCriteriaEntity> findByPriority(int priority);

  List<MembershipTierCriteriaEntity> findAllOrderedByPriority();

  MembershipTierCriteriaEntity save(MembershipTierCriteriaEntity criteria);

  void flush();
}
