package com.firstclub.membership.data.repository;

import com.firstclub.membership.data.entity.MembershipPlanEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MembershipPlanRepository {
  Page<MembershipPlanEntity> findByMembershipTierId(UUID membershipTierId, Pageable pageable);

  List<MembershipPlanEntity> findAllByMembershipTierId(UUID membershipTierId);

  List<MembershipPlanEntity> findAllByIds(java.util.Set<UUID> ids);

  List<MembershipPlanEntity> findAvailableByMembershipTierIds(
      java.util.Set<UUID> membershipTierIds);

  MembershipPlanEntity save(MembershipPlanEntity plan);

  Optional<MembershipPlanEntity> findById(UUID id);
}
