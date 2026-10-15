package com.firstclub.membership.data.dao;

import com.firstclub.membership.data.entity.RefundAllocationEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RefundAllocationDao {
  @PersistenceContext private EntityManager entityManager;

  public RefundAllocationEntity save(RefundAllocationEntity allocation) {
    entityManager.persist(allocation);
    return allocation;
  }

  public java.util.Optional<RefundAllocationEntity> findById(UUID id) {
    return java.util.Optional.ofNullable(entityManager.find(RefundAllocationEntity.class, id));
  }

  public java.util.Optional<RefundAllocationEntity> findByIdForUpdate(UUID id) {
    return java.util.Optional.ofNullable(
        entityManager.find(RefundAllocationEntity.class, id, LockModeType.PESSIMISTIC_WRITE));
  }

  public List<RefundAllocationEntity> findByRefundId(UUID refundId) {
    return entityManager
        .createQuery(
            "select allocation from RefundAllocationEntity allocation where allocation.refundId ="
                + " :refundId order by allocation.createdAt asc",
            RefundAllocationEntity.class)
        .setParameter("refundId", refundId)
        .getResultList();
  }

  /**
   * Pending allocations reserve capacity so the same gateway payment cannot be over-refunded
   * concurrently.
   */
  public BigDecimal sumReservedBySourcePaymentId(UUID sourcePaymentId) {
    return entityManager
        .createQuery(
            "select coalesce(sum(allocation.amount), 0.00) from RefundAllocationEntity allocation"
                + " where allocation.sourcePaymentId = :sourcePaymentId and allocation.status in"
                + " :statuses",
            BigDecimal.class)
        .setParameter("sourcePaymentId", sourcePaymentId)
        .setParameter(
            "statuses",
            List.of(
                com.firstclub.membership.models.RefundStatus.PENDING,
                com.firstclub.membership.models.RefundStatus.COMPLETED))
        .getSingleResult();
  }

  public java.util.Map<UUID, BigDecimal> sumReservedBySourcePaymentIds(
      java.util.Set<UUID> sourcePaymentIds) {
    if (sourcePaymentIds.isEmpty()) return java.util.Map.of();
    java.util.List<Object[]> rows =
        entityManager
            .createQuery(
                "select allocation.sourcePaymentId, coalesce(sum(allocation.amount), 0.00)"
                    + " from RefundAllocationEntity allocation where allocation.sourcePaymentId"
                    + " in :sourcePaymentIds and allocation.status in :statuses group by"
                    + " allocation.sourcePaymentId",
                Object[].class)
            .setParameter("sourcePaymentIds", sourcePaymentIds)
            .setParameter(
                "statuses",
                List.of(
                    com.firstclub.membership.models.RefundStatus.PENDING,
                    com.firstclub.membership.models.RefundStatus.COMPLETED))
            .getResultList();
    java.util.Map<UUID, BigDecimal> reserved = new java.util.HashMap<>();
    for (Object[] row : rows) reserved.put((UUID) row[0], (BigDecimal) row[1]);
    return reserved;
  }
}
