package com.firstclub.membership.core.command;

import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.core.query.MembershipPlanFetchService;
import com.firstclub.membership.core.query.MembershipSubscriptionFetchService;
import com.firstclub.membership.core.query.PaymentFetchService;
import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.models.PaymentStatus;
import com.firstclub.membership.models.PaymentType;
import com.firstclub.membership.models.SubscriptionStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Transactional lifecycle operations invoked by scheduled membership maintenance. */
@Service
public class MembershipMaintenanceManager {
  private final MembershipSubscriptionFetchService subscriptionFetchService;
  private final MembershipPlanFetchService planFetchService;
  private final PaymentFetchService paymentFetchService;
  private final PaymentWriteService paymentWriteService;

  public MembershipMaintenanceManager(
      MembershipSubscriptionFetchService subscriptionFetchService,
      MembershipPlanFetchService planFetchService,
      PaymentFetchService paymentFetchService,
      PaymentWriteService paymentWriteService) {
    this.subscriptionFetchService = subscriptionFetchService;
    this.planFetchService = planFetchService;
    this.paymentFetchService = paymentFetchService;
    this.paymentWriteService = paymentWriteService;
  }

  @Transactional
  public void runMaintenance(
      OffsetDateTime now, Duration autoRenewLeadTime, Duration paymentExpiry) {
    Instant maintenanceInstant = now.toInstant();
    expireMemberships(maintenanceInstant);
    createAutoRenewalPayments(maintenanceInstant, autoRenewLeadTime, paymentExpiry);
    abandonExpiredPendingCheckouts(maintenanceInstant);
  }

  @Transactional
  public void completeAutoRenewalPayment(
      MembershipSubscriptionEntity subscription, PaymentEntity payment, String bankReferenceId) {
    if (subscription.getStatus() != SubscriptionStatus.ACTIVE || !subscription.isAutoRenew()) {
      throw new IllegalStateException(
          "Only an active auto-renewing membership can complete a renewal payment");
    }
    MembershipPlanEntity plan =
        planFetchService.getRequired(subscription.getMembershipPlanId());
    subscription.extendExpiry(plan.getDurationDays());
    subscription.addAmountPaid(payment.getAmount());
    payment.complete(bankReferenceId);
    subscription.recordUpdate(com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
    payment.recordUpdate(com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
  }

  private void expireMemberships(Instant now) {
    subscriptionFetchService
        .findActiveExpiredBy(now)
        .forEach(
            subscription -> {
              subscription.expire();
              subscription.recordUpdate(
                  com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
            });
  }

  private void createAutoRenewalPayments(
      Instant now, Duration autoRenewLeadTime, Duration paymentExpiry) {
    Instant dueBy = now.plus(autoRenewLeadTime);
    var dueSubscriptions = subscriptionFetchService.findActiveAutoRenewDueBy(now, dueBy);
    var paymentsBySubscription = paymentsBySubscription(dueSubscriptions);
    var plansById = plansById(dueSubscriptions);
    dueSubscriptions.forEach(
        subscription ->
            createAutoRenewalPaymentIfNeeded(
                subscription, paymentsBySubscription, plansById, now, autoRenewLeadTime, paymentExpiry));
  }

  private java.util.Map<UUID, java.util.List<PaymentEntity>> paymentsBySubscription(
      java.util.List<MembershipSubscriptionEntity> subscriptions) {
    return paymentFetchService
        .findBySubscriptionIds(subscriptionIds(subscriptions))
        .stream()
        .collect(java.util.stream.Collectors.groupingBy(PaymentEntity::getSubscriptionId));
  }

  private java.util.Map<UUID, MembershipPlanEntity> plansById(
      java.util.List<MembershipSubscriptionEntity> subscriptions) {
    return planFetchService
        .findAllByIds(
            subscriptions.stream()
                .map(MembershipSubscriptionEntity::getMembershipPlanId)
                .collect(java.util.stream.Collectors.toSet()))
        .stream()
        .collect(java.util.stream.Collectors.toMap(MembershipPlanEntity::getId, plan -> plan));
  }

  private java.util.Set<UUID> subscriptionIds(
      java.util.List<MembershipSubscriptionEntity> subscriptions) {
    return subscriptions.stream()
        .map(MembershipSubscriptionEntity::getId)
        .collect(java.util.stream.Collectors.toSet());
  }

  private void createAutoRenewalPaymentIfNeeded(
      MembershipSubscriptionEntity subscription,
      java.util.Map<UUID, java.util.List<PaymentEntity>> paymentsBySubscription,
      java.util.Map<UUID, MembershipPlanEntity> plansById,
      Instant now,
      Duration autoRenewLeadTime,
      Duration paymentExpiry) {
    var payments = paymentsBySubscription.getOrDefault(subscription.getId(), java.util.List.of());
    if (hasPendingDirectPayment(payments, now)
        || hasAutoRenewalAttemptInCurrentWindow(payments, subscription, autoRenewLeadTime)) {
      return;
    }
    createAutoRenewalPayment(subscription, requirePlan(plansById, subscription), now, paymentExpiry);
  }

  private boolean hasPendingDirectPayment(java.util.List<PaymentEntity> payments, Instant now) {
    return payments.stream()
        .filter(payment -> payment.getPaymentType() == PaymentType.DIRECT_PAYMENT)
        .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
        .anyMatch(payment -> isPendingPayment(payment, now));
  }

  private boolean isPendingPayment(PaymentEntity payment, Instant now) {
    if (payment.getPaymentExpiresAt() == null || payment.getPaymentExpiresAt().isAfter(now)) {
      return true;
    }
    payment.expire();
    payment.recordUpdate(com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
    return false;
  }

  private MembershipPlanEntity requirePlan(
      java.util.Map<UUID, MembershipPlanEntity> plansById,
      MembershipSubscriptionEntity subscription) {
    MembershipPlanEntity plan = plansById.get(subscription.getMembershipPlanId());
    if (plan == null) {
      throw new ResourceNotFoundException("Membership plan", subscription.getMembershipPlanId());
    }
    return plan;
  }

  private void createAutoRenewalPayment(
      MembershipSubscriptionEntity subscription,
      MembershipPlanEntity plan,
      Instant now,
      Duration paymentExpiry) {
    PaymentEntity payment =
        new PaymentEntity(
            subscription.getId(),
            UUID.randomUUID().toString(),
            plan.getAmount(),
            subscription.getCurrencyCode(),
            now.plus(paymentExpiry));
    payment.recordCreation(com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
    paymentWriteService.save(payment);
  }

  private boolean hasAutoRenewalAttemptInCurrentWindow(
      java.util.List<PaymentEntity> payments,
      MembershipSubscriptionEntity subscription,
      Duration autoRenewLeadTime) {
    Instant renewalWindowStart = subscription.getExpiryTimestamp().minus(autoRenewLeadTime);
    return payments.stream()
        .filter(payment -> payment.getPaymentType() == PaymentType.DIRECT_PAYMENT)
        .map(PaymentEntity::getCreatedAt)
        .filter(java.util.Objects::nonNull)
        .anyMatch(createdAt -> !createdAt.isBefore(renewalWindowStart));
  }

  private void abandonExpiredPendingCheckouts(Instant now) {
    var expiredPendingSubscriptions =
        subscriptionFetchService.findPendingPaymentWithExpiredCheckoutAttemptBy(now);
    var paymentsBySubscription =
        paymentFetchService
            .findBySubscriptionIds(
                expiredPendingSubscriptions.stream()
                    .map(MembershipSubscriptionEntity::getId)
                    .collect(java.util.stream.Collectors.toSet()))
            .stream()
            .collect(java.util.stream.Collectors.groupingBy(PaymentEntity::getSubscriptionId));
    expiredPendingSubscriptions.forEach(
        subscription -> {
          voidPaymentsForExpiredCheckout(
              paymentsBySubscription.getOrDefault(subscription.getId(), java.util.List.of()));
          subscription.cancel(now);
          subscription.recordUpdate(com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
        });
  }

  private void voidPaymentsForExpiredCheckout(java.util.List<PaymentEntity> payments) {
    payments.forEach(this::voidPaymentForExpiredCheckout);
  }

  private void voidPaymentForExpiredCheckout(PaymentEntity payment) {
    if (isPendingDirectPayment(payment) || isSuccessfulMembershipCredit(payment)) {
      payment.voidPayment();
      payment.recordUpdate(com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
    }
  }

  private boolean isPendingDirectPayment(PaymentEntity payment) {
    return payment.getPaymentType() == PaymentType.DIRECT_PAYMENT
        && payment.getStatus() == PaymentStatus.PENDING;
  }

  private boolean isSuccessfulMembershipCredit(PaymentEntity payment) {
    return payment.getPaymentType() == PaymentType.MEMBERSHIP_CREDIT
        && payment.getStatus() == PaymentStatus.SUCCESS;
  }
}
