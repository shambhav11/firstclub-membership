package com.firstclub.membership.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.firstclub.membership.data.entity.MembershipPlanEntity;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.entity.PaymentEntity;
import com.firstclub.membership.core.query.MembershipPlanFetchService;
import com.firstclub.membership.core.query.MembershipSubscriptionFetchService;
import com.firstclub.membership.core.query.PaymentFetchService;
import com.firstclub.membership.models.PaymentStatus;
import com.firstclub.membership.models.PaymentType;
import com.firstclub.membership.models.PlanStatus;
import com.firstclub.membership.models.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class MembershipMaintenanceManagerTest {
  private final MembershipSubscriptionFetchService subscriptions =
      mock(MembershipSubscriptionFetchService.class);
  private final MembershipPlanFetchService plans = mock(MembershipPlanFetchService.class);
  private final PaymentFetchService payments = mock(PaymentFetchService.class);
  private final PaymentWriteService paymentWrites = mock(PaymentWriteService.class);
  private final MembershipMaintenanceManager service =
      new MembershipMaintenanceManager(subscriptions, plans, payments, paymentWrites);

  @Test
  void createsOneRenewalPaymentForAnEligibleMembership() {
    Instant now = Instant.parse("2026-10-04T12:00:00Z");
    MembershipSubscriptionEntity subscription = activeSubscription(now.plus(Duration.ofHours(12)));
    MembershipPlanEntity plan = plan(new BigDecimal("500.00"), 30);
    ReflectionTestUtils.setField(plan, "id", subscription.getMembershipPlanId());
    when(subscriptions.findActiveExpiredBy(now)).thenReturn(List.of());
    when(subscriptions.findActiveAutoRenewDueBy(now, now.plus(Duration.ofDays(1))))
        .thenReturn(List.of(subscription));
    when(subscriptions.findPendingPaymentWithExpiredCheckoutAttemptBy(now)).thenReturn(List.of());
    when(payments.findBySubscriptionIds(any())).thenReturn(List.of());
    when(plans.findAllByIds(any())).thenReturn(List.of(plan));

    service.runMaintenance(
        OffsetDateTime.ofInstant(now, ZoneOffset.ofHoursMinutes(5, 30)),
        Duration.ofDays(1),
        Duration.ofMinutes(15));

    ArgumentCaptor<PaymentEntity> payment = ArgumentCaptor.forClass(PaymentEntity.class);
    verify(paymentWrites).save(payment.capture());
    assertEquals(subscription.getId(), payment.getValue().getSubscriptionId());
    assertEquals(new BigDecimal("500.00"), payment.getValue().getAmount());
    assertEquals(PaymentStatus.PENDING, payment.getValue().getStatus());
    assertEquals(PaymentType.DIRECT_PAYMENT, payment.getValue().getPaymentType());
    assertEquals(now.plus(Duration.ofMinutes(15)), payment.getValue().getPaymentExpiresAt());
  }

  @Test
  void doesNotCreateAnotherRenewalWhileOneIsPending() {
    Instant now = Instant.parse("2026-10-04T12:00:00Z");
    MembershipSubscriptionEntity subscription = activeSubscription(now.plus(Duration.ofHours(12)));
    PaymentEntity pending =
        new PaymentEntity(
            subscription.getId(),
            "renewal",
            new BigDecimal("500.00"),
            "INR",
            now.plus(Duration.ofMinutes(15)));
    when(subscriptions.findActiveExpiredBy(now)).thenReturn(List.of());
    when(subscriptions.findActiveAutoRenewDueBy(now, now.plus(Duration.ofDays(1))))
        .thenReturn(List.of(subscription));
    when(subscriptions.findPendingPaymentWithExpiredCheckoutAttemptBy(now)).thenReturn(List.of());
    when(payments.findBySubscriptionIds(any())).thenReturn(List.of(pending));

    service.runMaintenance(
        OffsetDateTime.ofInstant(now, ZoneOffset.ofHoursMinutes(5, 30)),
        Duration.ofDays(1),
        Duration.ofMinutes(15));

    verify(paymentWrites, never()).save(any());
    assertEquals(PaymentStatus.PENDING, pending.getStatus());
  }

  @Test
  void completesRenewalByExtendingTheExistingMembership() {
    Instant previousExpiry = Instant.parse("2026-10-05T12:00:00Z");
    MembershipSubscriptionEntity subscription = activeSubscription(previousExpiry);
    MembershipPlanEntity plan = plan(new BigDecimal("500.00"), 30);
    PaymentEntity renewal =
        new PaymentEntity(
            subscription.getId(), "renewal", new BigDecimal("500.00"), "INR", previousExpiry);
    when(plans.getRequired(subscription.getMembershipPlanId())).thenReturn(plan);

    service.completeAutoRenewalPayment(subscription, renewal, "bank-ref");

    assertEquals(previousExpiry.plus(Duration.ofDays(30)), subscription.getExpiryTimestamp());
    assertEquals(new BigDecimal("1000.00"), subscription.getAmountPaid());
    assertEquals(PaymentStatus.SUCCESS, renewal.getStatus());
    assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
  }

  @Test
  void expiresAnActiveMembershipWhoseTermHasEnded() {
    Instant now = Instant.parse("2026-10-04T12:00:00Z");
    MembershipSubscriptionEntity expired = activeSubscription(now);
    when(subscriptions.findActiveExpiredBy(now)).thenReturn(List.of(expired));
    when(subscriptions.findActiveAutoRenewDueBy(now, now.plus(Duration.ofDays(1))))
        .thenReturn(List.of());
    when(subscriptions.findPendingPaymentWithExpiredCheckoutAttemptBy(now)).thenReturn(List.of());

    service.runMaintenance(
        OffsetDateTime.ofInstant(now, ZoneOffset.ofHoursMinutes(5, 30)),
        Duration.ofDays(1),
        Duration.ofMinutes(15));

    assertEquals(SubscriptionStatus.EXPIRED, expired.getStatus());
    org.junit.jupiter.api.Assertions.assertFalse(expired.isAutoRenew());
  }

  @Test
  void voidsAnAbandonedReplacementAndItsCreditWithoutTouchingTheActiveMembership() {
    Instant now = Instant.parse("2026-10-04T12:00:00Z");
    MembershipSubscriptionEntity pendingReplacement = pendingSubscription();
    MembershipSubscriptionEntity existingActive = activeSubscription(now.plus(Duration.ofDays(20)));
    PaymentEntity pendingDirect =
        new PaymentEntity(
            pendingReplacement.getId(),
            "replacement",
            new BigDecimal("100.00"),
            "INR",
            now.minusSeconds(1));
    PaymentEntity credit =
        PaymentEntity.membershipCredit(
            pendingReplacement.getId(),
            new BigDecimal("400.00"),
            "INR",
            existingActive.getId(),
            UUID.randomUUID());
    when(subscriptions.findActiveExpiredBy(now)).thenReturn(List.of());
    when(subscriptions.findActiveAutoRenewDueBy(now, now.plus(Duration.ofDays(1))))
        .thenReturn(List.of());
    when(subscriptions.findPendingPaymentWithExpiredCheckoutAttemptBy(now))
        .thenReturn(List.of(pendingReplacement));
    when(payments.findBySubscriptionIds(any())).thenReturn(List.of(pendingDirect, credit));

    service.runMaintenance(
        OffsetDateTime.ofInstant(now, ZoneOffset.ofHoursMinutes(5, 30)),
        Duration.ofDays(1),
        Duration.ofMinutes(15));

    assertEquals(PaymentStatus.VOIDED, pendingDirect.getStatus());
    assertEquals(PaymentStatus.VOIDED, credit.getStatus());
    assertEquals(SubscriptionStatus.CANCELLED, pendingReplacement.getStatus());
    assertEquals(SubscriptionStatus.ACTIVE, existingActive.getStatus());
    assertEquals(now, pendingReplacement.getCancelledAt());
  }

  @Test
  void voidsCreditWhenTheLatestFailedReplacementPaymentHasExpired() {
    Instant now = Instant.parse("2026-10-04T12:00:00Z");
    MembershipSubscriptionEntity pendingReplacement = pendingSubscription();
    MembershipSubscriptionEntity existingActive = activeSubscription(now.plus(Duration.ofDays(20)));
    PaymentEntity failedDirect =
        new PaymentEntity(
            pendingReplacement.getId(),
            "replacement",
            new BigDecimal("100.00"),
            "INR",
            now.minusSeconds(1));
    failedDirect.fail();
    PaymentEntity credit =
        PaymentEntity.membershipCredit(
            pendingReplacement.getId(),
            new BigDecimal("400.00"),
            "INR",
            existingActive.getId(),
            UUID.randomUUID());
    when(subscriptions.findActiveExpiredBy(now)).thenReturn(List.of());
    when(subscriptions.findActiveAutoRenewDueBy(now, now.plus(Duration.ofDays(1))))
        .thenReturn(List.of());
    when(subscriptions.findPendingPaymentWithExpiredCheckoutAttemptBy(now))
        .thenReturn(List.of(pendingReplacement));
    when(payments.findBySubscriptionIds(any())).thenReturn(List.of(failedDirect, credit));

    service.runMaintenance(
        OffsetDateTime.ofInstant(now, ZoneOffset.ofHoursMinutes(5, 30)),
        Duration.ofDays(1),
        Duration.ofMinutes(15));

    assertEquals(PaymentStatus.FAILED, failedDirect.getStatus());
    assertEquals(PaymentStatus.VOIDED, credit.getStatus());
    assertEquals(SubscriptionStatus.CANCELLED, pendingReplacement.getStatus());
  }

  private MembershipSubscriptionEntity activeSubscription(Instant expiry) {
    MembershipSubscriptionEntity subscription =
        new MembershipSubscriptionEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("500.00"),
            BigDecimal.ZERO,
            "INR",
            true);
    ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
    subscription.activate(expiry.minus(Duration.ofDays(30)), expiry, new BigDecimal("500.00"));
    return subscription;
  }

  private MembershipSubscriptionEntity pendingSubscription() {
    MembershipSubscriptionEntity subscription =
        new MembershipSubscriptionEntity(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("500.00"),
            new BigDecimal("400.00"),
            "INR",
            true);
    ReflectionTestUtils.setField(subscription, "id", UUID.randomUUID());
    return subscription;
  }

  private MembershipPlanEntity plan(BigDecimal amount, int durationDays) {
    MembershipPlanEntity plan =
        new MembershipPlanEntity(
            UUID.randomUUID(), durationDays, "Monthly", amount, "INR", PlanStatus.ACTIVE, false);
    ReflectionTestUtils.setField(plan, "id", UUID.randomUUID());
    return plan;
  }
}
