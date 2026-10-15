package com.firstclub.membership.core.query;

import com.firstclub.membership.data.entity.RefundEntity;
import com.firstclub.membership.data.repository.RefundRepository;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Focused refund reads backed only by the refund repository. */
@Service
@Transactional(readOnly = true)
public class RefundFetchService {
  private final RefundRepository refundRepository;

  public RefundFetchService(RefundRepository refundRepository) {
    this.refundRepository = refundRepository;
  }

  public Optional<RefundEntity> findBySubscriptionId(UUID subscriptionId) {
    return refundRepository.findBySubscriptionId(subscriptionId);
  }

  public Optional<RefundEntity> findByIdForUpdate(UUID refundId) {
    return refundRepository.findByIdForUpdate(refundId);
  }

  public Optional<RefundEntity> findById(UUID refundId) {
    return refundRepository.findById(refundId);
  }

  public Map<UUID, RefundEntity> findBySubscriptionIds(Set<UUID> subscriptionIds) {
    return refundRepository.findBySubscriptionIds(subscriptionIds).stream()
        .collect(
            java.util.stream.Collectors.toMap(RefundEntity::getSubscriptionId, refund -> refund));
  }
}
