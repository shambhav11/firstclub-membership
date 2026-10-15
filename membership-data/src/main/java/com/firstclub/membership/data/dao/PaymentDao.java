package com.firstclub.membership.data.dao;

import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.models.PaymentType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentDao {
  @PersistenceContext private EntityManager entityManager;

  public Optional<PaymentEntity> findById(UUID id) {
    return Optional.ofNullable(entityManager.find(PaymentEntity.class, id));
  }

  public Optional<PaymentEntity> findByIdForUpdate(UUID id) {
    return Optional.ofNullable(entityManager.find(PaymentEntity.class, id, LockModeType.PESSIMISTIC_WRITE));
  }

  public List<PaymentEntity> findByIds(java.util.Set<UUID> ids) {
    if (ids.isEmpty()) return List.of();
    return entityManager
        .createQuery(
            "select payment from PaymentEntity payment where payment.id in :ids", PaymentEntity.class)
        .setParameter("ids", ids)
        .getResultList();
  }

  public PaymentEntity save(PaymentEntity payment) {
    entityManager.persist(payment);
    return payment;
  }

  public Optional<PaymentEntity> findLatestBySubscriptionId(UUID subscriptionId) {
    return entityManager
        .createQuery(
            "select payment from PaymentEntity payment where payment.subscriptionId ="
                + " :subscriptionId order by payment.createdAt desc",
            PaymentEntity.class)
        .setParameter("subscriptionId", subscriptionId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  public List<PaymentEntity> findLatestBySubscriptionIds(java.util.Set<UUID> subscriptionIds) {
    if (subscriptionIds.isEmpty()) return List.of();
    return entityManager
        .createQuery(
            "select payment from PaymentEntity payment where payment.subscriptionId in"
                + " :subscriptionIds order by payment.subscriptionId asc, payment.createdAt desc",
            PaymentEntity.class)
        .setParameter("subscriptionIds", subscriptionIds)
        .getResultList();
  }

  public Optional<PaymentEntity> findSuccessfulBySubscriptionId(UUID subscriptionId) {
    return entityManager
        .createQuery(
            "select payment from PaymentEntity payment where payment.subscriptionId ="
                + " :subscriptionId and payment.status = :status order by payment.createdAt desc",
            PaymentEntity.class)
        .setParameter("subscriptionId", subscriptionId)
        .setParameter("status", com.firstclub.membership.models.PaymentStatus.SUCCESS)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  public List<PaymentEntity> findSuccessfulDirectPaymentsBySubscriptionId(UUID subscriptionId) {
    return entityManager
        .createQuery(
            "select payment from PaymentEntity payment where payment.subscriptionId ="
                + " :subscriptionId and payment.status = :status and payment.paymentType ="
                + " :paymentType order by payment.createdAt desc",
            PaymentEntity.class)
        .setParameter("subscriptionId", subscriptionId)
        .setParameter("status", com.firstclub.membership.models.PaymentStatus.SUCCESS)
        .setParameter("paymentType", PaymentType.DIRECT_PAYMENT)
        .getResultList();
  }

  public List<PaymentEntity> findSuccessfulPaymentsBySubscriptionId(UUID subscriptionId) {
    return entityManager
        .createQuery(
            "select payment from PaymentEntity payment where payment.subscriptionId ="
                + " :subscriptionId and payment.status = :status order by payment.createdAt desc",
            PaymentEntity.class)
        .setParameter("subscriptionId", subscriptionId)
        .setParameter("status", com.firstclub.membership.models.PaymentStatus.SUCCESS)
        .getResultList();
  }

  public List<PaymentEntity> findBySubscriptionIdAndStatus(
      UUID subscriptionId, com.firstclub.membership.models.PaymentStatus status) {
    return entityManager
        .createQuery(
            "select payment from PaymentEntity payment where payment.subscriptionId ="
                + " :subscriptionId and payment.status = :status",
            PaymentEntity.class)
        .setParameter("subscriptionId", subscriptionId)
        .setParameter("status", status)
        .getResultList();
  }

  public List<PaymentEntity> findBySubscriptionId(UUID subscriptionId) {
    return entityManager
        .createQuery(
            "select payment from PaymentEntity payment where payment.subscriptionId ="
                + " :subscriptionId",
            PaymentEntity.class)
        .setParameter("subscriptionId", subscriptionId)
        .getResultList();
  }

  public List<PaymentEntity> findBySubscriptionIds(java.util.Set<UUID> subscriptionIds) {
    if (subscriptionIds.isEmpty()) return List.of();
    return entityManager
        .createQuery(
            "select payment from PaymentEntity payment where payment.subscriptionId in"
                + " :subscriptionIds",
            PaymentEntity.class)
        .setParameter("subscriptionIds", subscriptionIds)
        .getResultList();
  }
}
