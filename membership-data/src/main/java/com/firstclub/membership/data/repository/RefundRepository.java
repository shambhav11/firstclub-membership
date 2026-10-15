package com.firstclub.membership.data.repository;

import com.firstclub.membership.data.entity.RefundEntity;
import java.util.Optional;
import java.util.UUID;

public interface RefundRepository {
  Optional<RefundEntity> findBySubscriptionId(UUID subscriptionId);

  java.util.List<RefundEntity> findBySubscriptionIds(java.util.Set<UUID> subscriptionIds);

  Optional<RefundEntity> findById(UUID id);

  Optional<RefundEntity> findByIdForUpdate(UUID id);

  RefundEntity save(RefundEntity refund);
}
