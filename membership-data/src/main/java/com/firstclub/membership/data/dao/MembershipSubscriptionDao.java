package com.firstclub.membership.data.dao;

import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.models.PaymentStatus;
import com.firstclub.membership.models.PaymentType;
import com.firstclub.membership.models.SubscriptionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class MembershipSubscriptionDao {
  @PersistenceContext private EntityManager entityManager;

  public Optional<MembershipSubscriptionEntity> findByUserIdAndStatus(
      UUID userId, SubscriptionStatus status) {
    return entityManager
        .createQuery(
            "select subscription from MembershipSubscriptionEntity subscription "
                + "where subscription.userId = :userId and subscription.status = :status",
            MembershipSubscriptionEntity.class)
        .setParameter("userId", userId)
        .setParameter("status", status)
        .getResultStream()
        .findFirst();
  }

  public Optional<MembershipSubscriptionEntity> findById(UUID id) {
    return Optional.ofNullable(entityManager.find(MembershipSubscriptionEntity.class, id));
  }

  public MembershipSubscriptionEntity save(MembershipSubscriptionEntity subscription) {
    entityManager.persist(subscription);
    return subscription;
  }

  public void flush() {
    entityManager.flush();
  }

  public Page<MembershipSubscriptionEntity> findByUserId(UUID userId, Pageable pageable) {
    List<MembershipSubscriptionEntity> content =
        entityManager
            .createQuery(
                "select subscription from MembershipSubscriptionEntity subscription where"
                    + " subscription.userId = :userId order by subscription.createdAt desc",
                MembershipSubscriptionEntity.class)
            .setParameter("userId", userId)
            .setFirstResult((int) pageable.getOffset())
            .setMaxResults(pageable.getPageSize())
            .getResultList();
    Long total =
        entityManager
            .createQuery(
                "select count(subscription) from MembershipSubscriptionEntity subscription where"
                    + " subscription.userId = :userId",
                Long.class)
            .setParameter("userId", userId)
            .getSingleResult();
    return new PageImpl<>(content, pageable, total);
  }

  public Optional<MembershipSubscriptionEntity> findLatestByUserId(UUID userId) {
    return entityManager
        .createQuery(
            "select subscription from MembershipSubscriptionEntity subscription where"
                + " subscription.userId = :userId order by subscription.createdAt desc",
            MembershipSubscriptionEntity.class)
        .setParameter("userId", userId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  public List<MembershipSubscriptionEntity> findActiveAutoRenewDueBy(
      Instant fromExclusive, Instant dueByInclusive) {
    return entityManager
        .createQuery(
            "select subscription from MembershipSubscriptionEntity subscription "
                + "where subscription.status = :status and subscription.autoRenew = true "
                + "and subscription.expiryTimestamp > :fromExclusive "
                + "and subscription.expiryTimestamp <= :dueByInclusive",
            MembershipSubscriptionEntity.class)
        .setParameter("status", SubscriptionStatus.ACTIVE)
        .setParameter("fromExclusive", fromExclusive)
        .setParameter("dueByInclusive", dueByInclusive)
        .getResultList();
  }

  public List<MembershipSubscriptionEntity> findActiveExpiredBy(Instant expiryInclusive) {
    return entityManager
        .createQuery(
            "select subscription from MembershipSubscriptionEntity subscription where"
                + " subscription.status = :status and subscription.expiryTimestamp <="
                + " :expiryInclusive",
            MembershipSubscriptionEntity.class)
        .setParameter("status", SubscriptionStatus.ACTIVE)
        .setParameter("expiryInclusive", expiryInclusive)
        .getResultList();
  }

  public List<MembershipSubscriptionEntity> findPendingPaymentWithExpiredCheckoutAttemptBy(
      Instant paymentExpiryInclusive) {
    return entityManager
        .createQuery(
            "select subscription from MembershipSubscriptionEntity subscription where"
                + " subscription.status = :subscriptionStatus and exists (select directPayment.id"
                + " from PaymentEntity directPayment where directPayment.subscriptionId ="
                + " subscription.id and directPayment.paymentType = :directPaymentType and"
                + " directPayment.paymentExpiresAt is not null and"
                + " directPayment.paymentExpiresAt <= :paymentExpiryInclusive and"
                + " directPayment.createdAt = (select max(latestDirectPayment.createdAt) from"
                + " PaymentEntity latestDirectPayment where latestDirectPayment.subscriptionId ="
                + " subscription.id and latestDirectPayment.paymentType = :directPaymentType)"
                + " and (directPayment.status = :pendingPaymentStatus or"
                + " (directPayment.status = :failedPaymentStatus and exists (select credit.id"
                + " from PaymentEntity credit where credit.subscriptionId = subscription.id and"
                + " credit.paymentType = :creditPaymentType and credit.status ="
                + " :successfulPaymentStatus))))",
            MembershipSubscriptionEntity.class)
        .setParameter("subscriptionStatus", SubscriptionStatus.PENDING_PAYMENT)
        .setParameter("directPaymentType", PaymentType.DIRECT_PAYMENT)
        .setParameter("creditPaymentType", PaymentType.MEMBERSHIP_CREDIT)
        .setParameter("pendingPaymentStatus", PaymentStatus.PENDING)
        .setParameter("failedPaymentStatus", PaymentStatus.FAILED)
        .setParameter("successfulPaymentStatus", PaymentStatus.SUCCESS)
        .setParameter("paymentExpiryInclusive", paymentExpiryInclusive)
        .getResultList();
  }
}
