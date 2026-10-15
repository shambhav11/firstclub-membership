package com.firstclub.membership.data.dao;

import com.firstclub.membership.data.entity.RefundEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RefundDao {
  @PersistenceContext private EntityManager entityManager;

  public Optional<RefundEntity> findBySubscriptionId(UUID subscriptionId) {
    return entityManager
        .createQuery(
            "select refund from RefundEntity refund where refund.subscriptionId = :subscriptionId",
            RefundEntity.class)
        .setParameter("subscriptionId", subscriptionId)
        .getResultStream()
        .findFirst();
  }

  public java.util.List<RefundEntity> findBySubscriptionIds(java.util.Set<UUID> subscriptionIds) {
    if (subscriptionIds.isEmpty()) return java.util.List.of();
    return entityManager
        .createQuery(
            "select refund from RefundEntity refund where refund.subscriptionId in"
                + " :subscriptionIds",
            RefundEntity.class)
        .setParameter("subscriptionIds", subscriptionIds)
        .getResultList();
  }

  public Optional<RefundEntity> findById(UUID id) {
    return Optional.ofNullable(entityManager.find(RefundEntity.class, id));
  }

  public Optional<RefundEntity> findByIdForUpdate(UUID id) {
    return Optional.ofNullable(entityManager.find(RefundEntity.class, id, LockModeType.PESSIMISTIC_WRITE));
  }

  public RefundEntity save(RefundEntity refund) {
    entityManager.persist(refund);
    return refund;
  }
}
