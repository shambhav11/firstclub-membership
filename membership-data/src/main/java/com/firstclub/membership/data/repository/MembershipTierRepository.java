package com.firstclub.membership.data.repository;

import com.firstclub.membership.data.entity.MembershipTierEntity;
import com.firstclub.membership.models.TierStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MembershipTierRepository {
  Optional<MembershipTierEntity> findById(UUID id);

  List<MembershipTierEntity> findAllByIds(java.util.Set<UUID> ids);

  Optional<MembershipTierEntity> findByDisplayNameIgnoreCase(String displayName);

  MembershipTierEntity save(MembershipTierEntity tier);

  void flush();

  Page<MembershipTierEntity> findPage(TierStatus status, Pageable pageable);

  List<MembershipTierEntity> findAllAvailableForEnrollment();
}
